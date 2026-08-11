"""
批量登录拿 token（绕过图形验证码）
==================================
原理：直连 Redis 写入验证码明文 captcha:{uuid}=1234，再调登录接口拿 JWT。
输出：tokens.txt（每行一个 token），供 locustfile.py 加载。

用法：
  python gen_tokens.py --count 50 --accounts user1:pass1,user2:pass2
  python gen_tokens.py --count 100 --accounts 15800000001:123456 --gateway-url http://localhost:8080

参数：
  --count         生成 token 数量
  --accounts      账号列表，格式 username:password,username:password（轮询使用）
  --redis-host    Redis 地址，默认 127.0.0.1
  --redis-port    Redis 端口，默认 6379
  --redis-db      Redis 库，默认 0
  --gateway-url   网关地址，默认 http://localhost:8080
  --output        输出文件，默认 tokens.txt

注意：账号需已注册且状态正常。token 有效期 24h，足够压测使用。
"""
import argparse
import uuid
import requests
import redis

CAPTCHA_PREFIX = "captcha:"


def gen_tokens(args):
    r = redis.Redis(host=args.redis_host, port=args.redis_port, db=args.redis_db, decode_responses=True)
    accounts = [a.strip() for a in args.accounts.split(",") if a.strip()]
    if not accounts:
        print("[错误] 未提供账号")
        return

    tokens = []
    for i in range(args.count):
        username, password = accounts[i % len(accounts)].split(":")
        captcha_uuid = str(uuid.uuid4())
        # 写入验证码明文，5 分钟过期（对齐服务端 CAPTCHA_EXPIRE_MINUTES）
        r.set(CAPTCHA_PREFIX + captcha_uuid, "1234", ex=300)

        resp = requests.post(f"{args.gateway_url}/api/auth/login", json={
            "username": username,
            "password": password,
            "captcha": "1234",
            "captchaUuid": captcha_uuid,
        }, timeout=10)
        data = resp.json()
        token = data.get("data", {}).get("accessToken")
        if token:
            tokens.append(token)
            print(f"[{i + 1}/{args.count}] {username} 登录成功")
        else:
            print(f"[{i + 1}/{args.count}] {username} 登录失败: {data.get('message')}")

    with open(args.output, "w", encoding="utf-8") as f:
        f.write("\n".join(tokens))
    print(f"\n完成: {len(tokens)}/{args.count} 个 token 写入 {args.output}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="批量登录拿 token")
    parser.add_argument("--count", type=int, default=50, help="生成 token 数量")
    parser.add_argument("--accounts", required=True, help="账号列表 user:pass,user:pass")
    parser.add_argument("--redis-host", default="127.0.0.1")
    parser.add_argument("--redis-port", type=int, default=6379)
    parser.add_argument("--redis-db", type=int, default=0)
    parser.add_argument("--gateway-url", default="http://localhost:8080")
    parser.add_argument("--output", default="tokens.txt")
    gen_tokens(parser.parse_args())
