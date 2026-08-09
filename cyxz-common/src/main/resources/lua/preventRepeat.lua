-- 防重复提交 Lua 脚本
-- KEYS[1] = 防重 key
-- ARGV[1] = 过期时间（秒）
-- ARGV[2] = 存储的值（固定 "1"）
-- 返回：1 = 首次请求（设置成功，放行）；0 = 重复请求（key 已存在，拦截）
-- 原子性：Redis 保证脚本执行期间不被其他命令打断，避免 "检查+设置" 并发缝隙

if redis.call('SET', KEYS[1], ARGV[2], 'NX', 'EX', ARGV[1]) then
    return 1
else
    return 0
end
