# 模块架构与 B 端演进

> 最后更新：2026-08-10

## 一、现有模块概览

### C 端业务服务（9 个）

| 模块 | 端口 | 职责 | 评级 |
|---|---|---|---|
| cyxz-gateway | 9000 | 网关：路由、鉴权、信任头防伪造剥离、`/internal` 防外泄 | 良好 |
| cyxz-auth | 9001 | 认证授权：RBAC（6 角色 22 权限码）、登录、JWT | 良好 |
| cyxz-user | 9002 | 用户资料、关注/粉丝社交图谱 | 良好 |
| cyxz-post | 9003 | 帖子：状态机、AI 三级审核、AC 自动机、ES CQRS、计数刷库 | 偏重，已减肥 |
| cyxz-comment | 9004 | 评论：独立聚合根 | 合理 |
| cyxz-circle | 9005 | 圈子：模板+关联两层模型、权限评估器 | 合理（循环依赖已破除） |
| cyxz-message | 9007 | 消息：私信（WebSocket）+ 通知（事件驱动） | 建议中期拆 chat/notify |
| cyxz-search | 9008 | 搜索：post 的 CQRS 读侧 | 合理 |
| cyxz-upload | 9009 | 上传：基础设施型 | 合理 |

### B 端治理服务（新增）

| 模块 | 端口 | 职责 | 状态 |
|---|---|---|---|
| cyxz-governance | 9006 | 内容治理中心：举报与内容处置 | 已建（建圈/入圈申请已迁入 circle） |

### 共享模块（2 个）

| 模块 | 评价 |
|---|---|
| cyxz-common | 职责适中（Result/BaseEntity/Redis/Rabbit/Mybatis 配置 + AbstractCountFlushService 基类 + AbstractManualAckRabbitListener + AbstractDlxRabbitConfig）。注意别让它变上帝模块 |
| cyxz-security | 抽离得当，加分项。BaseSecurityConfig + CirclePermissionEvaluator + HeaderAuthenticationFilter 集中安全能力 |

### *-api 模块（6 个 + governance-api）

方向对（Feign 契约 + DTO 放被调方）。cyxz-governance-api 承载治理事件 + 常量，供消费端引用。

## 二、架构亮点

1. **RBAC 资源评估器**：`@circlePerm` 基于 `sys_user_role` 跨库只读校验圈子权限
2. **AI 三级审核状态机**：草稿 → 待审 → AI 审核 → 人工复审 → 已通过/拒绝
3. **AC 自动机**：多模式敏感词匹配，O(N) 复杂度
4. **ES CQRS**：post 写 MySQL、异步同步 ES，search 服务独立读侧
5. **计数刷库**：`AbstractCountFlushService` 基类 + Redis 缓冲 + 定时刷库
6. **两套事件总线**：
   - PostCountEvent（post → circle，破循环依赖）
   - 治理事件总线（governance → circle，ContentTakedownEvent / CircleApprovedEvent / CircleJoinApprovedEvent）

## 三、已完成的架构修复（2026-08-10）

### 1. 破除 post ↔ circle 循环依赖【P0 硬伤】

**问题**：circle 通过 Feign 批量拉 post 计数，post 也依赖 circle，形成双向依赖。

**方案**：事件驱动替代 Feign 同步。

```
post 审核/删除
   ↓ 发布 PostCountEvent
circle 消费 → 原子更新 post_count
```

**改动**：
- common 新增 `PostCountEvent` + `PostCountConstants`
- post 在审核通过/删除时发事件
- circle 新增 `PostCountConsumer` 消费事件，`CircleMapper.updatePostCount` 原子更新
- circle 删除 `cyxz-post-api` 依赖、Feign 客户端、定时任务、`@EnableFeignClients`

### 2. PostController 拆分【P0】

**问题**：PostController 600 行混 admin 接口。

**方案**：5 个平台 admin 接口迁入 `PostAdminController`（`/admin/post` 前缀），圈子审核（圈主 C 端能力）保留原处。网关新增 `/api/admin/post/**` 路由。

## 四、B 端治理中心（cyxz-governance）

### 设计原则

> 治理中心只存申请/举报记录 + 发事件，不直接写业务库。

各业务服务消费事件完成自身状态更新，避免 governance 依赖多个业务数据库。

### 三条主线（已闭环）

#### 1. 举报处理

```
用户 POST /api/report 提交举报（targetType + targetId + reason）
   ↓ governance 保存 PENDING 举报（uk_reporter_target 防重复）
管理员 PUT /api/admin/report/{id}/approve 审核通过
   ↓ governance 发布 ContentTakedownEvent
post / comment 消费 → 删除内容（待接）
message 消费 → 通知举报人/作者（待接）
```

#### 2. 建圈申请（已迁入 circle 模块）

```
用户 POST /api/circle-application 提交建圈申请（name + intro + avatar + cover）
   ↓ circle 保存 PENDING 申请
管理员 PUT /api/admin/circle-application/{id}/approve 审核通过
   ↓ circle 直接调用 createCircle() 建圈 + 指定圈主 + 初始化板块（同模块同步事务）
```

#### 3. 入圈申请（已迁入 circle 模块）

```
用户 POST /api/circle-join-application 提交入圈申请（circleId + reason）
   ↓ circle 保存 PENDING 申请
管理员 PUT /api/admin/circle-join-application/{id}/approve 审核通过
   ↓ circle 直接调用 joinCircle() 加入成员 + 角色分配（同模块同步事务）
```

### RabbitMQ 拓扑

- Exchange：`cyxz.governance.exchange`（Topic）
- DLX：`cyxz.governance.dlx`
- 路由：`governance.takedown`（内容处置）
- 消费端手动 ACK，失败入死信队列

## 五、后续演进路线

### P1（高优先级，待办）

- **post/comment 消费 ContentTakedownEvent**：举报通过后自动删内容
- **message 通知**：举报结果双向通知举报人/内容作者

### P2（中优先级，架构亮点）

#### cyxz-audit：事件驱动审计

```
管理员操作 → 发布 ADMIN_OP 事件
   ↓ audit 消费 → 幂等写入审计日志
   ↓ 后台按人/类型/时间查询
```

注意：先称"事件驱动审计"，Outbox 可靠消息方案成熟后再称"事件溯源"。

#### cyxz-analytics：CQRS 读模型

```
业务服务产生事件
   ↓ analytics 消费 → 写入日统计表 / Redis 聚合计数
   ↓ 看板直接查询 analytics 本地读模型
```

第一版只做：用户每日注册数、帖子每日发布数、审核通过/拒绝数、圈子成员增长数、全站概览。

### P3（暂缓）

- cyxz-admin-bff（管理端聚合层，等出现 4+ 服务聚合需求再做）
- cyxz-ops-config（敏感词/AI策略/公告/全局参数，等配置明显增多再抽）
- cyxz-chat / cyxz-notify 拆分（等规模扩大）
- Zipkin 链路追踪
- 复杂 Saga 补偿机制

## 六、最终推荐结构

```
现有业务服务
├── cyxz-gateway
├── cyxz-auth
├── cyxz-user
├── cyxz-post
├── cyxz-comment
├── cyxz-circle
├── cyxz-message
├── cyxz-search
└── cyxz-upload

已新增
└── cyxz-governance   # 举报、建圈/入圈申请、处罚编排

待新增
├── cyxz-audit        # 事件驱动审计（P2）
└── cyxz-analytics    # CQRS 统计读模型（P2）

暂缓
├── cyxz-admin-bff
├── cyxz-ops-config
└── cyxz-chat / cyxz-notify
```
