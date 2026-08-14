import http from 'k6/http';
import { sleep } from 'k6';

// 压测配置：25s 爬升到 2500 VU，再持续 60s（对齐 Locust 的 -r 100 -t 60s）
export const options = {
  stages: [
    { duration: '25s', target: 2500 },
    { duration: '60s', target: 2500 },
  ],
};

const BASE_URL = 'http://localhost:8080';

// init 阶段：加载 token（open 结果缓存，全 VU 共享）
const tokens = open('./tokens.txt').split('\n').map(t => t.trim()).filter(t => t !== '');

// setup：跑一次，预取帖子 ID（列表是公开读，无需 token）
export function setup() {
  const res = http.get(`${BASE_URL}/api/post/list?sortBy=latest&page=1&size=20`, { tags: { name: '预热-帖子列表' } });
  let postIds = [1];
  if (res.status === 200) {
    try {
      const records = (res.json().data || {}).records || [];
      if (records.length > 0) postIds = records.map(r => r.id);
    } catch (e) {}
  }
  return { postIds };
}

// 每个 VU 反复执行：按权重随机选一个接口（读 5:3，写各 1）
export default function (data) {
  const postIds = data.postIds;
  const token = tokens[__VU % tokens.length];
  const authHeaders = token ? { Authorization: `Bearer ${token}` } : {};
  const pid = postIds[Math.floor(Math.random() * postIds.length)];

  const r = Math.random() * 12; // 总权重 = 5 + 3 + 1 + 1 + 1 + 1 = 12
  if (r < 5) {
    http.get(`${BASE_URL}/api/post/list?sortBy=latest&page=1&size=10`, { tags: { name: '读-帖子列表' } });
  } else if (r < 8) {
    http.get(`${BASE_URL}/api/post/${pid}`, { headers: authHeaders, tags: { name: '读-帖子详情' } });
  } else if (r < 9) {
    http.put(`${BASE_URL}/api/post/${pid}/like`, null, { headers: authHeaders, tags: { name: '写-点赞' } });
  } else if (r < 10) {
    http.del(`${BASE_URL}/api/post/${pid}/like`, null, { headers: authHeaders, tags: { name: '写-取消点赞' } });
  } else if (r < 11) {
    http.put(`${BASE_URL}/api/post/${pid}/collect`, null, { headers: authHeaders, tags: { name: '写-收藏' } });
  } else {
    http.del(`${BASE_URL}/api/post/${pid}/collect`, null, { headers: authHeaders, tags: { name: '写-取消收藏' } });
  }

  sleep(Math.random() * 2 + 1); // 1-3s 思考时间
}
