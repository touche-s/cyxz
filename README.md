# 次元社区 (Cyxz)

一个基于 Spring Cloud 微服务架构的二次元社区平台，支持用户注册登录、帖子发布与分类浏览、二级评论、点赞收藏、关注与粉丝管理、创作中心等核心功能。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.5.2 |
| 微服务 | Spring Cloud + Alibaba | 2025.0.0 / 2023.0.3.2 |
| 网关 | Spring Cloud Gateway | - |
| 注册中心 | Nacos | - |
| 数据库 | MySQL + MyBatis-Plus | - |
| 缓存 | Redis | - |
| 对象存储 | MinIO | - |
| 服务调用 | OpenFeign + FallbackFactory | - |
| 认证 | JWT + BCrypt | - |
| 前端 | Vue 3 + TypeScript | - |
| UI 框架 | Element Plus | 2.14 |
| 构建工具 | Vite | 8.1 |
| 状态管理 | Pinia | 3.0 |

## 项目结构

```
cyxz
├── cyxz-common          # 公共库：统一返回、异常处理、Redis/Feign 配置、@CurrentUser
├── cyxz-gateway         # 网关服务：路由转发、JWT 鉴权、CORS（8080）
├── cyxz-auth            # 认证服务：注册/登录/验证码、Token 签发
├── cyxz-auth-api        # Auth Feign 接口
├── cyxz-user            # 用户服务：资料管理、关注/取关
├── cyxz-user-api        # User Feign 接口 + VO/DTO
├── cyxz-post            # 帖子服务：CRUD、分类、点赞/收藏、浏览统计
├── cyxz-post-api        # Post Feign 接口
├── cyxz-comment         # 评论服务：二级评论、评论点赞
├── cyxz-upload          # 上传服务：MinIO 文件上传
├── cyxz-message         # 消息服务（规划中）
├── cyxz-search          # 搜索服务（规划中）
├── cyxz-frontend        # 前端：Vue 3 + Element Plus
└── db/                  # 数据库初始化脚本
```

## 功能清单

### 用户模块
- 注册/登录（验证码 + JWT）
- 个人资料编辑（头像上传、昵称、简介等）
- 个人空间（展示发帖、获赞、浏览量）
- 关注/取关 + 粉丝管理

### 帖子模块
- 发布/编辑/删除帖子（支持封面图）
- 分类浏览（动漫/游戏/绘画/COS/漫展/同人/周边/闲聊/资源）
- 点赞/收藏
- 浏览统计（Redis 去重 + 定时落库）

### 评论模块
- 两级评论（父评论 + 子回复）
- 子回复按需加载（避免全表扫描）
- 评论点赞
- 创作中心 → 评论管理（按帖子筛选、删除评论）

### 创作中心
- 数据概览（作品数/总浏览量/获赞数/评论数）
- 数字卡片滚动动画
- 内容管理（帖子列表、编辑/删除）
- 粉丝管理

### 其他
- 首页发现流
- 全局头像点击跳转个人空间
- 点赞/收藏弹跳动效
- 登录弹窗滑入动效
- B 站风格 UI 设计

## 数据库

4 个数据库、9 张表：

| 库 | 表 | 说明 |
|----|----|------|
| cyxz_auth | sys_user | 用户认证 |
| cyxz_user | user_profile, user_follow | 用户资料、关注关系 |
| cyxz_post | category, post, post_like, post_collect | 分类、帖子、点赞、收藏 |
| cyxz_comment | comment, comment_like | 评论、评论点赞 |

初始化脚本：[db/init.sql](db/init.sql)

## 本地运行

### 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 7+
- Nacos 2.x
- MinIO
- Node.js 20+

### 1. 初始化数据库

执行 `db/init.sql` 创建数据库和表。

### 2. 配置环境变量

复制 `.env.example` 为 `.env`，修改数据库、Redis、MinIO 等连接信息：

```bash
cp .env.example .env
```

### 3. 启动后端

按顺序启动服务：

```bash
# 1. 注册中心 (默认端口 8848)
./nacos/bin/startup.cmd -m standalone

# 2. 网关 + 各业务模块
./mvnw spring-boot:run -pl cyxz-gateway
./mvnw spring-boot:run -pl cyxz-auth
./mvnw spring-boot:run -pl cyxz-user
./mvnw spring-boot:run -pl cyxz-post
./mvnw spring-boot:run -pl cyxz-comment
./mvnw spring-boot:run -pl cyxz-upload
```

或使用 IDEA 同时启动所有模块。

### 4. 启动前端

```bash
cd cyxz-frontend
npm install
npm run dev
```

前端开发服务器默认运行在 `http://localhost:5173`，网关统一入口为 `http://localhost:8080`。

## 设计亮点

- **Feign 批量查询**：替代 N+1 查询，用户信息/帖子标题一次批量获取
- **评论二级分页**：子回复按需加载，避免全表扫描和 OOM
- **@CurrentUser 参数解析器**：自定义注解 + HandlerMethodArgumentResolver，Controller 无需手动解析 Token
- **FallbackFactory**：Feign 调用降级兜底，区分"无数据"和"服务不可用"
- **ID 全链路 String**：雪花 ID 转 String，防止前端 JavaScript 精度丢失
- **Redis 浏览统计**：Hash 增量 + 定时批量落库，避免频繁写 MySQL

## License

MIT
<img width="1274" height="626" alt="image" src="https://github.com/user-attachments/assets/aa8b0273-c889-41b8-8fff-eb2e3fc51bcd" />
<img width="1273" height="623" alt="image" src="https://github.com/user-attachments/assets/2142579a-544f-44a9-8775-68ab87006815" />
<img width="1277" height="622" alt="image" src="https://github.com/user-attachments/assets/b92972bf-c97d-4d9e-aa0c-acf623b88ab4" />


