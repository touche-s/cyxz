# Cyxz 部署指南

本文档涵盖两种部署方式：**Docker 一键部署**（推荐新人，拉代码即跑）和**本地开发模式**（适合二次开发调试）。

---

## 一、Docker 一键部署（推荐）

只需安装 Docker Desktop，无需本地配置 JDK / MySQL / Redis 等环境。编排 21 个服务：7 基础设施（含 minio-init one-shot）+ 12 Java 微服务 + 前端 + AI 审核，稳态运行 20 个容器。

### 前置条件

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)（含 Docker Compose，建议分配 **8GB+ 内存**）

### 步骤

```bash
# 1. 克隆仓库
git clone <repo-url>
cd cyxz/docker

# 2. 配置环境变量（必须，compose 用 :? 校验必填项，不配会报错）
cp .env.example .env
#   编辑 .env，至少设置以下项：
#     DB_PASSWORD        MySQL root 密码
#     MINIO_ACCESS_KEY   MinIO 访问密钥
#     MINIO_SECRET_KEY   MinIO 访问密钥（≥ 8 位）
#     JWT_SECRET         JWT 签名密钥（≥ 32 字节，否则启动报错）
#     LLM_API_KEY        AI 审核密钥（留空则默认拒绝/转人工审核，fail-closed；如需自动放行可设 AUTO_PASS_WITHOUT_KEY=true）

# 3. 一键构建并启动（PowerShell）
.\deploy.ps1
#   或手动：
#   docker build -f maven-deps.Dockerfile -t cyxz-maven-deps:latest ..
#   docker compose up -d --build

# 4. 验证（首次构建约 10-15 分钟）
docker compose ps
#   20 个容器 Up / Healthy 即成功（minio-init 执行完退出，属正常）
```

### 访问地址

| 服务 | 地址 | 账号 |
|------|------|------|
| 前端 | http://localhost:80 | 注册即可用；内置站主 `admin / Admin@123`（演示账号，请尽快改密） |
| API 网关 | http://localhost:8080 | - |
| Nacos 控制台 | http://localhost:8848/nacos | nacos / nacos |
| RabbitMQ 管理台 | http://localhost:15672 | guest / guest |
| MinIO 控制台 | http://localhost:9001 | 见 `.env` 中 MINIO_ACCESS_KEY / SECRET_KEY |
| Elasticsearch | http://localhost:9200 | 无认证 |

> 端口冲突时，在 `.env` 中修改 `FRONTEND_PORT` / `GATEWAY_PORT` / `MYSQL_PORT` 等映射端口。

### 常用命令

```bash
cd docker

# 查看状态
docker compose ps

# 查看某服务日志（实时跟踪）
docker compose logs -f gateway

# 代码变更后重新构建单个服务
docker compose build auth && docker compose up -d auth

# pom / 公共模块变更后，先重建依赖镜像再构建
docker build -f maven-deps.Dockerfile -t cyxz-maven-deps:latest ..
docker compose up -d --build

# 停止全部服务（数据保留）
docker compose down

# 停止并清除所有数据卷（完全重置）
docker compose down -v
```

### 构建说明

采用多阶段构建 + 依赖预构建镜像优化速度：

- **`maven-deps.Dockerfile`** — 预装所有 Maven 依赖 + 公共模块 jar 到本地仓库。pom 或公共模块变更时才需重建，其余时间层缓存命中，秒级完成。
- **`microservice.Dockerfile`** — `FROM cyxz-maven-deps`，只编译目标服务（`-pl`），无需重复下载依赖。
- **`es.Dockerfile`** — ES 8.13.0 + IK 中文分词插件。
- **`frontend.Dockerfile`** — Vite 构建 + Nginx 托管静态资源。
- **`ai.Dockerfile`** — Python FastAPI AI 审核服务。

### 服务编排概览

| 层级 | 服务 | 依赖 |
|------|------|------|
| 基础设施 | mysql, redis, rabbitmq, minio, minio-init, elasticsearch, nacos | - |
| 微服务 | gateway, auth, user, post, comment, circle, message, search, upload, governance, audit, analytics | nacos + 各自中间件 |
| AI 审核 | ai | rabbitmq |
| 前端 | frontend | gateway |

所有服务通过 `cyxz-net` Bridge 网络互通，数据持久化使用 Docker 命名卷（mysql-data / redis-data / rabbitmq-data / minio-data / es-data / nacos-data）。

> **注意**：所有微服务必须连接同一 MySQL 实例。圈子服务（circle）的 `@Transactional` 通过跨库表名前缀写入 `cyxz_auth.sys_user_role`，权限校验也跨库读取 `cyxz_auth` / `cyxz_circle` 表，分库部署会导致事务失效和权限查询失败。

---

## 二、本地开发模式

适合二次开发：前端 Vite 热更新 + 后端 IDEA 直启，中间件用 Docker 跑。

### 前置条件

- JDK 17、Maven 3.9+、Node.js 22
- Docker Desktop（仅用于启动中间件）

### 1. 启动中间件

```bash
cd docker
cp .env.example .env          # 按需修改密码
# 只启动基础设施，不构建微服务镜像
docker compose up -d mysql redis rabbitmq nacos elasticsearch minio minio-init
```

MySQL 首次启动会自动执行 `db/init.sql` 建库建表。

### 2. 启动后端服务

各服务 `application.yml` 默认连接 `localhost`，开箱即用。唯一需要设置的环境变量是 `JWT_SECRET`（无默认值）：

```bash
# PowerShell（当前终端生效）
$env:JWT_SECRET = "your-jwt-secret-change-me-in-production"

# 或在 IDEA 启动配置中添加 Environment Variable: JWT_SECRET=...
```

按依赖顺序启动（建议 IDEA 配置多启动类一键运行）：

```bash
mvn spring-boot:run -pl cyxz-gateway      # 1. 网关
mvn spring-boot:run -pl cyxz-auth         # 2. 认证
mvn spring-boot:run -pl cyxz-user         # 3. 用户
mvn spring-boot:run -pl cyxz-post         # 4. 帖子
mvn spring-boot:run -pl cyxz-comment      # 5. 评论
mvn spring-boot:run -pl cyxz-circle       # 6. 圈子
mvn spring-boot:run -pl cyxz-message      # 7. 消息
mvn spring-boot:run -pl cyxz-search       # 8. 搜索
mvn spring-boot:run -pl cyxz-upload       # 9. 上传
mvn spring-boot:run -pl cyxz-governance   # 10. 治理
mvn spring-boot:run -pl cyxz-audit        # 11. 审计
mvn spring-boot:run -pl cyxz-analytics    # 12. 统计
```

> 本地开发端口：gateway 8080、auth 9001、user 9002、post 9003、comment 9004、circle 9005、upload 9006、message 9007、search 9008、governance 9009、audit 9010、analytics 9011。同时启动多个服务需保证端口互不冲突。

### 3. 启动 AI 审核服务（可选）

```bash
cd cyxz-ai
pip install -r requirements.txt
python main.py    # http://127.0.0.1:8000
```

未启动 AI 服务或未配置 LLM_API_KEY 时，默认 fail-closed（拒绝/转人工审核），帖子仍可正常发布（进入待审核状态）。如需无 AI 时自动放行，设置 `AUTO_PASS_WITHOUT_KEY=true`。

### 4. 启动前端

```bash
cd cyxz-frontend
npm install
npm run dev        # http://localhost:3000，/api 自动代理到 Gateway:8080
```

### 5. 测试

```bash
# 后端单元测试
mvn test -pl cyxz-post,cyxz-comment -am

# 前端测试
cd cyxz-frontend && npm test
```
