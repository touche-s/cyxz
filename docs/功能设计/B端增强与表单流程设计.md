# B 端增强与表单流程设计

> 2026-08-08 | 基于 [管理后台设计](./管理后台设计.md) 中规划的 P1/P2 能力，结合秋招简历展示需求，细化待实施功能的技术方案与业务闭环。

---

## 设计目标

在现有管理后台基础上（帖子审核、圈子管理、RBAC 权限分配），补齐以下能力，使管理后台从"内容审核工具"升级为"可审计、可运营、可回溯的管理系统"：

1. **操作审计闭环**：所有管理员敏感操作可追溯
2. **平台数据看板**：管理员视角的全站运营指标
3. **举报处理流程**：从用户举报到管理员处理到内容处置的完整链路
4. **评论审核**：补齐审核体系在评论层面的缺失
5. **入圈 / 入站申请**：表单填写 → 审核 → 结果通知 → 状态流转 → 操作审计
6. **角色 CRUD**：管理后台内新增/编辑/删除角色
7. **文件管理**：管理员视角的文件浏览与清理

> 评估标准：新人拉代码后，可以在管理后台完成"审核入圈申请 → 拒绝不当举报 → 查看操作审计详情 → 查看数据看板"一整套运营操作。

---

## 一、操作日志 / 审计日志（P0）

### 1.1 数据模型

```sql
CREATE TABLE operation_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id     BIGINT        NOT NULL COMMENT '操作人用户ID',
    operator_name   VARCHAR(50)   NOT NULL COMMENT '操作人昵称（冗余，便于展示）',
    target_type     VARCHAR(50)   NOT NULL COMMENT '操作对象类型：POST/COMMENT/CIRCLE/USER/REPORT/APPLICATION/ROLE',
    target_id       BIGINT        NULL     COMMENT '操作对象ID',
    action          VARCHAR(50)   NOT NULL COMMENT '操作类型：APPROVE/REJECT/DELETE/BAN/UNBAN/ASSIGN_ROLE/UPDATE/CREATE',
    before_status   VARCHAR(50)   NULL     COMMENT '变更前状态',
    after_status    VARCHAR(50)   NULL     COMMENT '变更后状态',
    detail          VARCHAR(500)  NULL     COMMENT '补充说明（如驳回原因、操作备注）',
    ip              VARCHAR(45)   NULL,
    user_agent      VARCHAR(500)  NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operator (operator_id, created_at),
    INDEX idx_target (target_type, target_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志';
```

### 1.2 记录策略

使用 AOP 切面 + 自定义注解，减少侵入式代码：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String targetType();    // POST / COMMENT / CIRCLE / USER / REPORT / APPLICATION / ROLE
    String action();        // APPROVE / REJECT / DELETE / BAN / UNBAN / ASSIGN_ROLE / UPDATE / CREATE
    String targetIdEl();    // Spring EL 表达式，如 "#postId" / "#report.id"
    String beforeStatusEl() default "";   // 可选：变更前状态
    String afterStatusEl()  default "";   // 可选：变更后状态
    String detailEl()       default "";   // 可选：补充说明
}
```

切面从 `SecurityContextHolder` 读取操作用户，通过 `HttpServletRequest` 获取 IP 和 UA，异步写入（线程池或 MQ），不阻塞主业务。

### 1.3 前端页面

AdminView 新增"操作日志"Tab：

| 列 | 说明 |
|---|---|
| 操作人 | 昵称 |
| 操作对象 | target_type + target_id（可点击跳转） |
| 操作类型 | action 中文映射 |
| 状态变更 | before → after |
| 说明 | detail |
| IP | 来源 IP |
| 时间 | created_at |

筛选：按操作人、操作类型、时间范围。

P2 可选：CSV 导出。

### 1.4 需记录的操作清单

| 操作 | target_type | action |
|------|-------------|--------|
| 审核通过帖子 | POST | APPROVE |
| 审核拒绝帖子 | POST | REJECT |
| 删除帖子 | POST | DELETE |
| 禁用/启用用户 | USER | BAN / UNBAN |
| 分配/变更用户角色 | USER | ASSIGN_ROLE |
| 创建圈子 | CIRCLE | CREATE |
| 更新圈子 | CIRCLE | UPDATE |
| 禁用/启用圈子 | CIRCLE | BAN / UNBAN |
| 删除圈子 | CIRCLE | DELETE |
| 审核通过评论 | COMMENT | APPROVE |
| 审核拒绝评论 | COMMENT | REJECT |
| 处理举报 | REPORT | APPROVE / REJECT |
| 审核通过入圈申请 | APPLICATION | APPROVE |
| 审核拒绝入圈申请 | APPLICATION | REJECT |
| 审核通过入站申请 | APPLICATION | APPROVE |
| 审核拒绝入站申请 | APPLICATION | REJECT |
| 任命/撤销圈子管理员 | CIRCLE | UPDATE |
| 更新角色权限 | ROLE | UPDATE |

---

## 二、管理员平台数据看板（P0）

### 2.1 接口设计

| 接口 | 说明 |
|------|------|
| `GET /admin/dashboard/overview` | 核心指标：总用户数、总帖子数、总圈子数、今日新增用户、今日新增帖子 |
| `GET /admin/dashboard/user-trend?days=30` | 近 N 天新注册用户趋势（折线图） |
| `GET /admin/dashboard/post-trend?days=30` | 近 N 天新发帖 + 审核通过趋势（柱状图） |
| `GET /admin/dashboard/circle-distribution` | 各圈子帖子数分布（饼图） |
| `GET /admin/dashboard/review-summary` | 审核概况：待审核数、今日通过数、今日拒绝数、积压天数 |

### 2.2 前端页面

AdminView 第一个 Tab 改为"工作台"，含：

- 顶部四卡片：总用户 / 总帖子 / 总圈子 / 待审核数
- 折线图：用户注册趋势
- 柱状图：发帖与审核趋势
- 饼图：圈子帖子分布
- 审核积压提醒（超过 24h 标红）

图表使用 ECharts（`npm install echarts vue-echarts`）。

### 2.3 数据来源

直接查 MySQL，不需要额外汇总表。30 天内数据量可接受（注册 + 发帖量级有限）。若后续数据量增大，再引入定时任务生成汇总表。

---

## 三、举报处理系统（P1）

### 3.1 数据模型

```sql
CREATE TABLE report (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id       BIGINT        NOT NULL COMMENT '举报人用户ID',
    target_type       VARCHAR(20)   NOT NULL COMMENT '举报对象类型：POST / COMMENT',
    target_id         BIGINT        NOT NULL COMMENT '举报对象ID',
    reason            VARCHAR(200)  NOT NULL COMMENT '举报原因',
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
    handler_id        BIGINT        NULL     COMMENT '处理人用户ID',
    handler_note      VARCHAR(500)  NULL     COMMENT '处理意见',
    handled_at        DATETIME      NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status, created_at),
    INDEX idx_reporter (reporter_id),
    UNIQUE KEY uk_reporter_target (reporter_id, target_type, target_id) COMMENT '同一用户对同一对象仅可举报一次'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报记录';
```

### 3.2 接口设计

**用户端：**

| 接口 | 说明 |
|------|------|
| `POST /report` | 提交举报（参数：targetType, targetId, reason） |

**管理端：**

| 接口 | 说明 |
|------|------|
| `GET /admin/report/list` | 举报列表（按状态/类型/时间筛选，分页） |
| `GET /admin/report/{id}` | 举报详情 + 被举报内容摘要 |
| `PUT /admin/report/{id}/approve` | 通过举报（必填处理意见） |
| `PUT /admin/report/{id}/reject` | 驳回举报（必填处理意见） |

### 3.3 处理逻辑

```
通过举报（approve）：
  1. 记录 handlerId + handlerNote + handledAt
  2. status → APPROVED
  3. 根据 targetType 隐藏/删除对应内容
     - POST → status = DELETED
     - COMMENT → status = DELETED
  4. 通知被举报者（可选）
  5. 通知举报者（处理结果）
  6. 记录操作日志

驳回举报（reject）：
  1. 记录 handlerId + handlerNote + handledAt
  2. status → REJECTED
  3. 通知举报者（驳回原因）
  4. 记录操作日志
```

### 3.4 前端页面

AdminView 新增"举报处理"Tab（替代当前内容审核 Tab 中举报功能的缺失）：

| 列 | 说明 |
|---|---|
| 举报人 | 昵称 |
| 举报对象 | 帖子标题/评论摘要（可点击查看） |
| 举报原因 | reason |
| 当前状态 | PENDING / APPROVED / REJECTED |
| 处理人 | 管理员昵称 |
| 处理时间 | handled_at |
| 操作 | 查看详情 → 通过 / 驳回 |

详情弹窗：被举报内容上下文 + 举报原因 + 处理区（通过/驳回 + 处理意见必填）。

---

## 四、评论审核（P1）

### 4.1 接口设计

在现有 `CommentController` 基础上增加管理端接口：

| 接口 | 说明 |
|------|------|
| `GET /comment/admin/review/pending` | 待审核评论分页列表 |
| `PUT /comment/admin/review/{id}/approve` | 审核通过 |
| `PUT /comment/admin/review/{id}/reject` | 审核拒绝（带原因） |
| `GET /comment/admin/list` | 全量评论列表（按状态/关键词筛选） |
| `DELETE /comment/admin/{id}` | 管理员删除评论 |

### 4.2 前端页面

AdminView 内容审核 Tab 增加"评论审核"子标签，复用帖子审核的 UI 布局。

### 4.3 评论状态扩展

在 `Comment` 实体增加 `status` 字段，复用帖子的审核状态枚举：

```text
PENDING → APPROVED / REJECTED
```

与帖子审核共用 `PostStatus` 状态码体系，或独立定义 `CommentStatus`。

---

## 五、入圈申请（P1 - 新增）

> 不属于 [管理后台设计](./管理后台设计.md) 原有规划，是 B 端表单流程的核心新增。

### 5.1 业务场景

用户访问需要审核加入的圈子时，提交申请 → 圈主/管理员审核 → 通过后成为成员 / 驳回后通知申请人。

### 5.2 数据模型

```sql
CREATE TABLE circle_join_application (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_id    BIGINT        NOT NULL COMMENT '申请人用户ID',
    circle_id       BIGINT        NOT NULL COMMENT '目标圈子ID',
    reason          VARCHAR(500)  NOT NULL COMMENT '申请理由',
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED / CANCELLED',
    reviewer_id     BIGINT        NULL     COMMENT '审核人用户ID',
    reject_reason   VARCHAR(500)  NULL     COMMENT '驳回原因',
    reviewed_at     DATETIME      NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_circle_status (circle_id, status, created_at),
    INDEX idx_applicant (applicant_id, circle_id),
    UNIQUE KEY uk_applicant_circle (applicant_id, circle_id) COMMENT '同一用户同一圈子仅一条有效申请'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入圈申请表';
```

### 5.3 申请规则

| 规则 | 说明 |
|------|------|
| 重复申请拦截 | 同一用户同一圈子已有 PENDING 申请时，返回"已有待审核申请" |
| 已是成员拦截 | 已是该圈子成员（含圈主/管理员/普通成员）时禁止申请 |
| 驳回后重试 | 驳回后允许重新提交（更新已有记录而非新增） |
| 圈子状态检查 | 圈子被禁用时不允许新申请，已有 PENDING 申请保留不动 |
| 撤回 | 申请人可在 PENDING 状态下撤回（status → CANCELLED） |

### 5.4 接口设计

**用户端：**

| 接口 | 说明 |
|------|------|
| `POST /circle/{circleId}/apply` | 提交入圈申请（参数：reason） |
| `PUT /circle/{circleId}/application/cancel` | 撤回申请 |
| `GET /circle/{circleId}/my-application` | 查看自己的申请状态 |

**管理端（圈子管理员/圈主/平台管理员）：**

| 接口 | 说明 |
|------|------|
| `GET /circle/{circleId}/admin/applications` | 本圈申请列表（按状态/时间筛选，分页） |
| `GET /circle/{circleId}/admin/applications/{id}` | 申请详情 |
| `PUT /circle/{circleId}/admin/applications/{id}/approve` | 审核通过（自动写入圈子成员关系） |
| `PUT /circle/{circleId}/admin/applications/{id}/reject` | 审核拒绝（必填驳回原因） |

### 5.5 审核通过流程

```
1. 校验申请存在且状态为 PENDING
2. 校验申请人当前不是该圈成员
3. 防重复：使用唯一索引 uk_user_circle 保证幂等
4. 写入 sys_user_role（role_code = CIRCLE_MEMBER, circle_id = 目标圈子）
5. 更新申请状态 → APPROVED，记录 reviewer 和 reviewed_at
6. 发送通知给申请人
7. 记录操作日志
8. 失效该申请人对应圈子的权限缓存
```

### 5.6 前端页面

**用户端**：圈子加入按钮旁 / 圈子详情页弹出申请表单（理由输入框 + 提交）。

**管理端**：CircleAdminView 新增"入圈申请"Tab：

| 列 | 说明 |
|---|---|
| 申请人 | 昵称、头像 |
| 申请时间 | created_at |
| 申请理由 | 截断展示，点击查看详情 |
| 当前状态 | PENDING / APPROVED / REJECTED / CANCELLED |
| 处理人 | reviewer 昵称 |
| 处理时间 | reviewed_at |
| 操作 | 通过 / 拒绝 + 填写原因（弹窗） |

---

## 六、入站申请（P2 - 新增）

> 不属于 [管理后台设计](./管理后台设计.md) 原有规划，是 B 端表单流程的第二项新增。

### 6.1 业务场景

用户注册后申请加入正式社区、创作者计划或特殊身份群体，由平台管理员审核。**不替代注册流程**，而是注册之后的身份准入。

### 6.2 数据模型

与入圈申请结构类似，可复用或独立建表：

```sql
CREATE TABLE admission_application (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_id    BIGINT        NOT NULL COMMENT '申请人用户ID',
    apply_type      VARCHAR(50)   NOT NULL DEFAULT 'CREATOR' COMMENT '申请类型：CREATOR（创作者）/ COMMUNITY（正式社区成员）',
    reason          VARCHAR(500)  NOT NULL COMMENT '申请理由',
    extra_data      JSON          NULL     COMMENT '附加字段（兴趣方向、创作经历等）',
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED / CANCELLED',
    reviewer_id     BIGINT        NULL,
    reject_reason   VARCHAR(500)  NULL,
    reviewed_at     DATETIME      NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status, created_at),
    UNIQUE KEY uk_applicant_type (applicant_id, apply_type) COMMENT '同一用户同一申请类型仅一条有效记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入站申请表';
```

### 6.3 接口设计

**用户端：**

| 接口 | 说明 |
|------|------|
| `POST /admission/apply` | 提交入站申请 |
| `PUT /admission/apply/cancel` | 撤回申请 |
| `GET /admission/my-application` | 查看申请状态 |

**管理端（平台管理员）：**

| 接口 | 说明 |
|------|------|
| `GET /admin/admission/list` | 申请列表 |
| `PUT /admin/admission/{id}/approve` | 审核通过（分配对应角色） |
| `PUT /admin/admission/{id}/reject` | 审核拒绝 |

### 6.4 前端页面

AdminView 新增"入站申请"Tab，UI 与入圈申请审核列表一致。

---

## 七、角色 CRUD（P2）

在 RoleAdminController 已有 "查看角色列表 + 分配权限" 基础上补充：

| 接口 | 说明 |
|------|------|
| `POST /auth/admin/roles` | 创建角色（参数：角色名、角色 code、类型） |
| `PUT /auth/admin/roles/{id}` | 编辑角色 |
| `DELETE /auth/admin/roles/{id}` | 删除角色（需检查是否有用户持有该角色） |

前端 AdminView "角色权限"Tab 对应补充新增/编辑/删除按钮与弹窗。

---

## 八、文件管理页面（P2）

### 8.1 接口设计

在 UploadController 增加管理端接口：

| 接口 | 说明 |
|------|------|
| `GET /admin/files` | 文件列表（按时间/类型/上传者筛选，分页） |
| `DELETE /admin/files/{key}` | 删除文件（从 MinIO + 数据库记录中删除） |
| `DELETE /admin/files/batch` | 批量删除 |
| `GET /admin/files/stats` | 存储统计：总文件数、总占用空间、按类型分布 |

### 8.2 前端页面

AdminView 新增"文件管理"Tab：

| 列 | 说明 |
|---|---|
| 缩略图 | 图片类型显示缩略图 |
| 文件名 | 原始文件名 |
| 类型 | 头像 / 帖子图片 / 圈子资源 |
| 大小 | 文件大小 |
| 上传者 | 用户昵称 |
| 上传时间 | created_at |
| 操作 | 预览（图片）/ 下载 / 删除 |

---

## 九、实施优先级与顺序

```
当前已有（P0 已实现）
  ├─ 管理员鉴权（@PreAuthorize + 权限码）
  ├─ 帖子审核（平台级 + 圈子级 + AI 自动）
  ├─ 帖子状态机（5 状态 + 流转表）
  ├─ 圈子管理（CRUD + 状态 + 板块模板）
  ├─ 用户管理（列表 + 启用/禁用 + 角色分配）
  ├─ RBAC 权限分配（6 角色 + 22 权限码 + 可视化面板）
  ├─ 创作者个人数据仪表盘
  └─ 敏感词检测

第一轮（操作审计 + 数据可观测 → 约 2-3 天）
  ├─ 1. 操作日志 / 审计日志
  └─ 2. 管理员平台数据看板

第二轮（审核闭环 + 举报流程 → 约 2-3 天）
  ├─ 3. 评论审核
  ├─ 4. 举报处理系统
  └─ 5. 入圈申请（含用户端表单 + 管理端审核 + 成员写入 + 通知）

第三轮（完整性补齐 → 约 1-2 天）
  ├─ 6. 角色 CRUD
  ├─ 7. 入站申请
  └─ 8. 文件管理页面
```

> 第一轮做完即可显著提升 B 端展示面；第二轮做完形成完整的"表单 → 审核 → 处理 → 通知 → 审计"链路；第三轮补齐框架完整性。

---

## 十、技术要点提醒

1. **操作日志必须异步**：用 `@Async` 线程池或 RabbitMQ 写入，日志落库失败不能阻塞主业务。
2. **入圈申请审核 → 写成员关系必须幂等**：依赖 `sys_user_role` 的唯一索引兜底，防止审核同一申请多次导致重复成员。
3. **权限校验复用现有体系**：所有管理端接口加 `@PreAuthorize` 对应权限码，不另做鉴权方案。
4. **入圈申请与圈子成员加入的区别**：当前已有 `joinCircle` 直接加入；入圈申请适用于"需要审核"的圈子；可在 Circle 表增加 `join_mode` 字段（DIRECT / REVIEW）区分。
5. **所有管理端接口返回统一 `Result<T>`**：保持与现有代码一致。
6. **审核操作全部记录操作日志**：通过 / 拒绝 / 删除 三步缺一不可。
