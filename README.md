# 次元小站 (Cyxz)

面向 ACGN 爱好者的轻量级社区平台，支持多圈子内容发布、社交互动与创作管理。采用 Spring Cloud 微服务架构，前后端分离，面向学习与个人项目展示。

## 技术栈

| 层级 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.5 + Spring Cloud 2025 + Spring Cloud Alibaba |
| 服务治理 | Nacos（注册发现 + 配置中心）、Spring Cloud Gateway |
| 安全鉴权 | JWT（仅存 userId）+ Redis Cache-Aside 权限缓存 + Spring Security |
| 数据层 | MySQL 8 + MyBatis-Plus、Redis、MinIO |
| 消息队列 | RabbitMQ（通知异步、ES 数据同步、死信队列） |
| 搜索引擎 | Elasticsearch 8 + IK 中文分词 |
| 实时通信 | WebSocket（私信推送、在线状态） |
| AI 审核 | 独立 Python 服务（FastAPI） |
| 前端 | Vue 3 + TypeScript + Vite + Pinia |
| 组件库 | Element Plus + Iconify |
| 部署 | Docker Compose 一键编排（17 容器） |
| CI | GitHub Actions |

## 系统架构

```mermaid
graph TB
    subgraph 前端
        FE[Vue 3 前端]
    end

    subgraph 网关层
        GW[Gateway<br/>路由 / JWT 验签 / 注入 X-User-Id]
    end

    subgraph 业务服务
        AUTH[认证服务]
        USER[用户服务]
        POST[帖子服务]
        COMMENT[评论服务]
        CIRCLE[圈子服务]
        MESSAGE[消息服务]
        SEARCH[搜索服务]
        UPLOAD[上传服务]
    end

    subgraph 基础设施
        MYSQL[(MySQL)]
        REDIS[(Redis<br/>权限缓存)]
        ES[(Elasticsearch)]
        MQ[[RabbitMQ]]
        MINIO[(MinIO)]
        NACOS[Nacos]
        AI[AI 审核服务<br/>Python]
    end

    FE --> GW
    GW --> AUTH & USER & POST & COMMENT & CIRCLE & MESSAGE & SEARCH & UPLOAD

    POST -. Feign .-> USER & COMMENT & CIRCLE
    POST -. 异步 MQ .-> MESSAGE & SEARCH & AI
    COMMENT -. Feign .-> USER
    MESSAGE -. Feign .-> USER

    AUTH --> MYSQL & REDIS & NACOS
    USER --> MYSQL & REDIS & MQ & NACOS
    POST --> MYSQL & REDIS & MQ & MINIO & NACOS
    COMMENT --> MYSQL & REDIS & MQ & NACOS
    CIRCLE --> MYSQL & REDIS & NACOS
    MESSAGE --> MYSQL & REDIS & MQ & NACOS
    SEARCH --> ES & MQ & NACOS
    UPLOAD --> MINIO & NACOS
```

### 鉴权链路

```
客户端 → Gateway（JWT 验签，剥离角色/权限头，仅注入 X-User-Id）
       → HeaderAuthenticationFilter（按 userId 从 Redis 加载权限到 SecurityContext）
       → AdminRoleFilter（管理员角色校验）
       → 业务 Controller（@CurrentUser / @AdminUser / @circlePerm 注解）
```

JWT 精简为仅存 userId，角色/权限码通过 Redis Cache-Aside 按需加载，变更时旁路删 key 失效缓存。

## 核心功能

- **用户系统**：注册登录、JWT 鉴权、图形验证码、个人资料管理
- **内容创作**：发帖（普通帖/文章帖）、草稿、发布前敏感词检测、AI 内容审核
- **圈子体系**：多圈子、板块模板、圈子成员管理、圈子帖子计数
- **社交互动**：点赞、评论（含楼中楼）、收藏、关注/粉丝
- **消息通知**：点赞/评论/关注通知、未读数、WebSocket 实时推送
- **私信系统**：一对一私信、会话列表、在线状态
- **全文搜索**：ES 索引同步（MQ 异步）、IK 中文分词、关键词高亮
- **创作中心**：作品数据统计、互动趋势、月度报表
- **管理后台**：用户管理、帖子审核、圈子管理、角色权限管理

## 项目结构

```
cyxz/
├── cyxz-gateway/        # API 网关（路由、JWT 验签、注入 X-User-Id）
├── cyxz-auth/           # 认证服务（登录注册、Token、登出清缓存）
├── cyxz-user/           # 用户服务（资料、关注关系）
├── cyxz-post/           # 帖子服务（发布、审核、互动、AI 审核）
├── cyxz-comment/        # 评论服务（评论、楼中楼、点赞）
├── cyxz-circle/         # 圈子服务（圈子、板块、成员、权限缓存失效）
├── cyxz-message/        # 消息服务（通知、私信、WebSocket）
├── cyxz-search/         # 搜索服务（ES 索引、全文检索）
├── cyxz-upload/         # 上传服务（MinIO 图床）
├── cyxz-common/         # 公共模块（异常、配置、常量、工具）
├── cyxz-security/       # 安全模块（权限缓存、Filter 链、权限注解）
├── cyxz-*-api/          # 各服务 Feign API + VO（auth/user/post/comment/circle/message）
├── cyxz-ai/             # AI 审核服务（Python / FastAPI）
├── cyxz-frontend/       # Vue 3 前端项目
├── db/                  # 数据库初始化脚本（init.sql）
├── docker/              # Docker 部署（compose、Dockerfile、DEPLOY.md）
├── docs/                # 设计文档
└── .github/workflows/   # CI 配置
```

## 技术亮点

> 以下为项目已落地的工程实践，持续迭代中。

- **权限 Cache-Aside**：JWT 精简为仅存 userId，角色/权限码通过 `GlobalPermissionProvider` 从 Redis 加载（Cache-Aside 模式），角色变更 / 圈子成员变更 / 登出时旁路删 key 失效缓存，避免权限头伪造风险
- **统一响应与异常处理**：`Result` + `BusinessException` + `GlobalExceptionHandler`，全局错误码管理，避免 try-catch 散落
- **Redis 缓存策略**：帖子详情 + 权限缓存均采用 Cache-Aside（写后失效），统一 key 命名规范（`CacheKeyConstants`）
- **RabbitMQ 异步解耦**：通知发送、ES 索引同步、AI 审核走 MQ，死信队列兜底消费失败消息
- **Feign 服务间调用**：`FallbackFactory` 统一降级，返回安全默认值，避免调用方异常处理模板
- **帖子状态机**：`PostStatus` 流转规则表 + `canTransition` 校验，乐观锁条件更新防止并发覆盖
- **跨服务最终一致性**：`TransactionSynchronizationManager.afterCommit` 事务提交后执行 Feign 调用与 MQ 发送，避免长事务持有 DB 连接及事务回滚后的幻象副作用；权限缓存删除统一走 `TransactionUtils.afterCommit` 封装
- **线程池隔离防雪崩**：AI 审核（秒级慢 HTTP）与帖子详情并行查询（毫秒级快任务）使用独立线程池（`aiReviewExecutor` / `postQueryExecutor`），避免 `ForkJoinPool.commonPool` 快慢任务混跑导致雪崩；RestTemplate 强制超时防止线程无限阻塞
- **ES 同步失败补偿**：MQ 发送失败时写入 Redis 失败队列，定时任务自动重试，避免生产端发送失败导致 DB-ES 永久不一致
- **权限提升防护**：站主（SITE_OWNER）账号禁止被平台管理员禁用/提权，内置角色权限禁止修改，角色分配受层级约束
- **MyBatis-Plus 自动填充**：`BaseEntity` 抽取公共字段 + `MyMetaObjectHandler` 自动填充创建/更新时间
- **定时计数汇总**：帖子/评论/圈子计数异步累加 + 定时刷库，避免实时写压力
- **Docker 一键部署**：多阶段构建 + maven-deps 依赖预构建镜像，17 容器一键编排，拉代码即跑

## 快速启动

提供两种方式，详细步骤见 [docker/DEPLOY.md](docker/DEPLOY.md)。

### Docker 一键部署（推荐新人）

```bash
cd docker
cp .env.example .env          # 编辑密码（DB_PASSWORD / MINIO / JWT_SECRET 等）
.\deploy.ps1                  # 一键构建并启动 17 个容器
docker compose ps             # 验证全部 Up / Healthy
```

访问 http://localhost:80 即可使用。前置条件仅需 [Docker Desktop](https://www.docker.com/products/docker-desktop/)（8GB+ 内存）。

### 本地开发模式

中间件用 Docker 跑，前端 Vite 热更新 + 后端 IDEA 直启：

```bash
cd docker && docker compose up -d mysql redis rabbitmq nacos elasticsearch minio minio-init

# 后端：设置 JWT_SECRET 环境变量后，按依赖顺序 mvn spring-boot:run -pl <service>
# 前端：cd cyxz-frontend && npm install && npm run dev   # http://localhost:3000
```

> 完整步骤、访问地址表、常用命令、构建说明见 [docker/DEPLOY.md](docker/DEPLOY.md)。

## CI/CD

项目使用 GitHub Actions 进行持续集成（见 `.github/workflows/ci.yml`）：

- **后端**：JDK 17 编译 + 单元测试（cyxz-post / cyxz-comment）
- **前端**：Node 20 安装依赖 + 单元测试 + 生产构建

## 设计文档

项目根目录 `docs/` 下记录了关键架构决策与迭代思考，均基于代码核对：

### 产品设计（`docs/产品设计/`）
- `产品设计.md` — 产品方向、用户画像、差异化定位与功能矩阵

### 功能设计（`docs/功能设计/`）
- `圈子化设计.md` — 圈子领域模型（模板 + 关联两层结构）与计数同步
- `消息通知与关注动态方案.md` — MQ 异步通知 + WebSocket 实时推送 + 私信
- `AI能力设计.md` — Python AI 审核服务（文本 + 图片多模态）与 fail-closed 策略
- `管理后台设计.md` — 两段式管理员鉴权 + 帖子状态机审核流程
- `权限设计.md` — 多租户 RBAC + SpringSecurity 方法级授权 + Cache-Aside 权限缓存
- `社区氛围功能设计.md` — 互动反馈层已实现 + 签到/成就/排行规划
- `多语言演进路线图.md` — 跨语言架构现状（Java + Python）与演进规划

## License

本项目基于 [MIT License](./LICENSE) 开源，仅供学习与个人使用。
