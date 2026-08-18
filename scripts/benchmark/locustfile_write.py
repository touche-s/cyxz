"""
cyxz 压测脚本（Locust）—— 仅写接口
====================================
压测目标：点赞 / 取消点赞 / 收藏 / 取消收藏（隔离写路径，排除读接口的 Feign 跨服务调用）

前置条件：
  1. 网关 + post 服务已启动（默认 http://localhost:8080）
  2. 已用 gen_tokens.py 生成 tokens.txt（写接口需要认证）

用法：
  locust -f locustfile_write.py --host=http://localhost:8080

环境变量：
  TOKENS_FILE  token 文件路径，默认同目录 tokens.txt

注意：
  - 点赞与取消点赞、收藏与取消收藏权重 1:1 交替执行，降低 @PreventRepeat(2s) 连续拦截
  - 每个虚拟用户分配独立 token，模拟多用户并发
"""
import os
import random
import itertools
from locust import HttpUser, task, between

TOKENS_FILE = os.environ.get("TOKENS_FILE", os.path.join(os.path.dirname(__file__), "tokens.txt"))


def load_tokens():
    if not os.path.exists(TOKENS_FILE):
        print(f"[警告] token 文件不存在: {TOKENS_FILE}，写接口将返回 401")
        return []
    with open(TOKENS_FILE, "r", encoding="utf-8") as f:
        tokens = [line.strip() for line in f if line.strip()]
    print(f"[信息] 加载 {len(tokens)} 个 token")
    return tokens


TOKENS = load_tokens()
_token_iter = itertools.cycle(TOKENS) if TOKENS else None


class WriteUser(HttpUser):
    """仅压写接口：点赞 / 取消点赞 / 收藏 / 取消收藏"""
    wait_time = between(1, 3)

    def on_start(self):
        if _token_iter:
            self.token = next(_token_iter)
            self.auth_headers = {"Authorization": f"Bearer {self.token}"}
        else:
            self.auth_headers = {}
        self.post_ids = self._fetch_post_ids()

    def _fetch_post_ids(self):
        """从帖子列表预取一批 postId 供写接口使用"""
        resp = self.client.get("/api/post/list?sortBy=latest&page=1&size=20", name="预热-帖子列表")
        ids = []
        if resp.status_code == 200:
            records = resp.json().get("data", {}).get("records", [])
            ids = [r["id"] for r in records if "id" in r]
        if not ids:
            print("[警告] 未取到帖子 ID，写接口将用 fallback postId=1")
            return [1]
        return ids

    @task(1)
    def like_post(self):
        """点赞帖子（需 token，@PreventRepeat 2s）"""
        pid = random.choice(self.post_ids)
        self.client.put(f"/api/post/{pid}/like", headers=self.auth_headers, name="写-点赞")

    @task(1)
    def unlike_post(self):
        """取消点赞（与点赞交替）"""
        pid = random.choice(self.post_ids)
        self.client.delete(f"/api/post/{pid}/like", headers=self.auth_headers, name="写-取消点赞")

    @task(1)
    def collect_post(self):
        """收藏帖子（需 token，@PreventRepeat 2s）"""
        pid = random.choice(self.post_ids)
        self.client.put(f"/api/post/{pid}/collect", headers=self.auth_headers, name="写-收藏")

    @task(1)
    def uncollect_post(self):
        """取消收藏（与收藏交替）"""
        pid = random.choice(self.post_ids)
        self.client.delete(f"/api/post/{pid}/collect", headers=self.auth_headers, name="写-取消收藏")
