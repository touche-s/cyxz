# 次元小站 (Cyxz)

面向 ACGN 爱好者的轻量级社区平台，支持多圈子内容发布、社交互动与创作管理。

## 技术栈

| 层级 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.5 + Spring Cloud 2025 |
| 服务治理 | Nacos（注册发现 + 配置中心）、Spring Cloud Gateway |
| 数据层 | MySQL + MyBatis-Plus、Redis、MinIO |
| 前端 | Vue 3 + TypeScript + Vite 8 + Pinia |
| 组件库 | Element Plus + Iconify |
| CI | GitHub Actions |

## 项目结构

```
cyxz/
├── cyxz-gateway/     # API 网关（路由、JWT 鉴权、CORS）
├── cyxz-auth/        # 认证服务（登录注册、Token 签发）
├── cyxz-user/        # 用户服务（资料、关注关系）
├── cyxz-post/        # 帖子服务（发布、圈子、点赞收藏）
├── cyxz-comment/     # 评论服务
├── cyxz-message/     # 消息通知服务
├── cyxz-search/      # 全文检索（ES 骨架已搭建）
├── cyxz-upload/      # 文件上传（MinIO）
├── cyxz-common/      # 公共模块（异常处理、Redis 配置等）
├── *-api/            # Feign 接口 + DTO（供服务间调用）
├── db/               # 数据库初始化脚本
└── cyxz-frontend/    # Vue 3 前端项目
```

## 快速启动

### 环境依赖

- JDK 17
- MySQL 8.0
- Redis
- Nacos 2.x
- MinIO
- Node.js 20

### 后端

```bash
# 1. 启动 Nacos、MySQL、Redis、MinIO
# 2. 执行 db/init.sql 初始化数据库
# 3. 按顺序启动服务
cd cyxz
mvn spring-boot:run -pl cyxz-gateway
mvn spring-boot:run -pl cyxz-auth
mvn spring-boot:run -pl cyxz-user
mvn spring-boot:run -pl cyxz-post
mvn spring-boot:run -pl cyxz-comment
mvn spring-boot:run -pl cyxz-message
mvn spring-boot:run -pl cyxz-upload
```

### 前端

```bash
cd cyxz-frontend
npm install
npm run dev        # 开发模式，默认 http://localhost:3000
npm run build      # 生产构建
```

### 测试

```bash
# 后端
mvn test -pl cyxz-post -am

# 前端
cd cyxz-frontend
npm test
```

## 设计文档

项目根目录下的设计文档记录了关键架构决策：

- `次元小站产品定位与垂直化设计思考.md` — 产品方向与差异化
- `次元小站圈子化改造后续方案.md` — 圈子体系设计
- `消息通知与关注动态方案.md` — 通知系统方案
- `次元小站下一步计划.md` — 迭代路线图

## License

仅供学习与个人使用。
