# 模块架构与 B 端演进

> 最后更新：2026-08-11

## 一、现有模块概览

### C 端业务服务（9 个）

| 模块 | 端口 | 职责 | 评级 |
|---|---|---|---|
| cyxz-gateway | 9000 | 网关：路由、鉴权、信任头防伪造剥离、`/internal` 防外泄 | 良好 |
| cyxz-auth | 9001 | 认证授权：RBAC（6 角色 35 权限码）、登录、JWT | 良好 |
| cyxz-user | 9002 | 用户资料、关注/粉丝社交图谱 | 良好 |
| cyxz-post | 9003 | 帖子：状态机、AI 三级审核、AC 自动机、ES CQRS、计数刷库 | 偏重，已减肥 |
| cyxz-comment | 9004 | 评论：独立聚合根 | 合理 |
| cyxz-circle | 9005 | 圈子：模板+关联两层模型、权限评估器、建圈/入圈申请 | 合理（循环依赖已破除，申请治理已闭环） |
| cyxz-message | 9007 | 消息：私信（WebSocket）+ 通知（事件驱动） | 建议中期拆 chat/notify |
| cyxz-search | 9008 | 搜索：post 的 CQRS 读侧 | 合理 |
| cyxz-upload | 9009 | 上传：基础设施型 | 合理 |

### B 端治理服务（3 个，已建成）

| 模块 | 端口 | 职责 | 状态 |
|---|---|---|---|
| cyxz-governance | 9006 | 内容治理中心：举报受理与处置编排 | 已建 |
| cyxz-audit | 9010 | 事件驱动审计：管理员敏操作日志 | 已建 |
| cyxz-analytics | 9011 | CQRS 统计读模型：数据看板 | 已建 |

### 共享模块（2 个）

| 模块 | 评价 |
|---|---|
| cyxz-common | 职责适中（Result/BaseEntity/Redis/Rabbit/Mybatis 配置 + AbstractCountFlushService 基类 + AbstractManualAckRabbitListener + AbstractDlxRabbitConfig）。注意别让它变上帝模块 |
| cyxz-security | 抽离得当，加分项。BaseSecurityConfig + CirclePermissionEvaluator + HeaderAuthenticationFilter 集中安全能力 |

### *-api 模块（8 个）

方向对（Feign 契约 + DTO 放被调方）。cyxz-governance-api、cyxz-audit-api 承载治理/审计事件与常量，供消费端引用。

---

## 二、架构亮点

1. **RBAC 资源评估器**：`@circlePerm` 基于 `sys_user_role` 跨库只读校验圈子权限
2. **AI 三级审核状态机**：草稿 → 待审 → AI 审核 → 人工复审 → 已通过/拒绝
3. **AC 自动机**：多模式敏感词匹配，O(N) 复杂度
4. **ES CQRS**：post 写 MySQL、异步同步 ES，search 服务独立读侧
5. **计数刷库**：`AbstractCountFlushService` 基类 + Redis 缓冲 + 定时刷库
6. **多套事件总线**：
   - PostCountEvent（post → circle，破循环依赖）
   - ContentTakedownEvent（governance → post/comment，举报处置）
   - AuditEvent（各服务 → audit，审计日志）
   - AnalyticsEvent（各服务 → analytics，数据看板聚合）
   - NotificationEvent（各服务 → message，通知推送）

---

## 三、已完成的关键架构修复

### 3.1 破除 post ↔ circle 循环依赖【2026-08-10】

**问题**：circle 通过 Feign 批量拉 post 计数，post 也依赖 circle，形成双向依赖。

**方案**：事件驱动替代 Feign 同步。

```
post 审核/删除
   ↓ 发布 PostCountEvent
circle 消费 → 原子更新 post_count
```

- circle 删除 `cyxz-post-api` 依赖、Feign 客户端、定时任务、`@EnableFeignClients`

### 3.2 PostController 拆分【2026-08-10】

5 个平台 admin 接口迁入 `PostAdminController`（`/admin/post` 前缀），圈子审核（圈主 C 端能力）保留原处。

### 3.3 建圈/入圈申请迁回 circle【2026-08-10】

原设计放在 governance，后发现审批结果直接创建圈子/成员关系，同模块本地事务即可保证一致性。迁回后 governance 专注举报治理与处置编排。

---

## 四、B 端治理中心（cyxz-governance）

### 设计原则

> 治理中心只存举报记录 + 发事件，不直接写业务库。

各业务服务消费事件完成自身状态更新，避免 governance 依赖多个业务数据库。

### 举报处理

```
用户 POST /api/report 提交举报（targetType + targetId + reason）
   ↓ governance 保存 PENDING 举报（uk_reporter_target 防重复）
管理员 PUT /api/admin/report/{id}/approve 审核通过
   ↓ governance 发布 ContentTakedownEvent
post / comment 消费 → 处置内容
```

### RabbitMQ 拓扑

- Exchange：`cyxz.governance.exchange`（Topic）
- DLX：`cyxz.governance.dlx`
- 路由：`governance.takedown`（内容处置）
- 消费端手动 ACK，失败入死信队列

---

## 五、数据看板（cyxz-analytics）

以 CQRS 读模型方式聚合全站统计：

```
业务服务写操作 → 发送 AnalyticsEvent → analytics 消费聚合 → 看板只查 analytics 本地读模型
```

已实现指标：新增用户、新增帖子、审核量、新建圈子、新增入圈、处理举报 + 7/14/30 天趋势。避免管理后台首页 Feign 调用六个业务服务。

---

## 六、审计日志（cyxz-audit）

事件驱动审计，避免每个业务服务重复建 `operation_log` 表：

```
管理员敏感操作 → 业务服务发布 AuditEvent → audit 消费并写入审计日志
```

已实现：按操作人/类型/时间检索，按时间范围筛选。

---

## 七、后续演进路线

### P1（高优先级）

- **post/comment 消费 ContentTakedownEvent**：举报通过后自动处置内容
- **举报详情增加内容上下文**：governance 通过 Feign 只读查询帖子/评论摘要
- **审计事件关联真实操作人**：从 SecurityContext 获取管理员 ID、IP

### P2（中优先级）

- cyxz-admin-bff（管理端聚合层，等出现 4+ 服务聚合需求再做）
- cyxz-ops-config（敏感词/AI策略/公告/全局参数，等配置明显增多再抽）

### P3（暂缓）

- cyxz-chat / cyxz-notify 拆分（等规模扩大）
- Zipkin 链路追踪
- 复杂 Saga 补偿机制
- 反馈工单系统

---

## 八、最终模块结构

```
现有业务服务（9 个）
├── cyxz-gateway
├── cyxz-auth
├── cyxz-user
├── cyxz-post
├── cyxz-comment
├── cyxz-circle
├── cyxz-message
├── cyxz-search
└── cyxz-upload

B 端治理服务（3 个，已建成）
├── cyxz-governance   # 举报受理与处置编排
├── cyxz-audit        # 事件驱动审计
└── cyxz-analytics    # CQRS 统计读模型

共享模块（2 个）
├── cyxz-common
└── cyxz-security

*-api 模块（8 个）
├── cyxz-auth-api
├── cyxz-user-api
├── cyxz-post-api
├── cyxz-comment-api
├── cyxz-message-api
├── cyxz-circle-api
├── cyxz-governance-api
└── cyxz-audit-api
```
