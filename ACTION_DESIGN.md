# 次元小站互动功能数据方案

## 1. 目标

本方案覆盖以下能力的数据设计与实现建议：

- 帖子点赞
- 帖子收藏
- 评论点赞
- 帖子评论数
- 帖子浏览数

目标是解决当前点赞/收藏/评论点赞使用 Redis Set 作为主存储带来的内存占用、数据持久化和后续扩展问题，同时规划浏览数和评论数的合理落地方式。

***

## 2. 当前方案存在的问题

当前帖子点赞、帖子收藏、评论点赞都采用 Redis Set 保存“用户-对象”的关系，例如：

- `user:liked:posts:{userId}`
- `user:collected:posts:{userId}`
- `user:liked:comments:{userId}`

这种方案在功能初期实现简单，但不适合作为长期主存储，主要问题如下：

### 2.1 内存压力会随业务增长持续放大

这类关系属于高基数、多对多数据：

- 用户越多，Key 越多
- 每个用户操作越多，Set 越大
- 点赞、收藏、评论点赞会各自存一份

随着数据增长，Redis 内存消耗会越来越明显，不适合作为全量关系数据的长期承载层。

### 2.2 Redis 更适合缓存，不适合做核心关系主存储

虽然 Redis 支持持久化，但它本质上更适合：

- 热点缓存
- 计数聚合
- 限流防刷
- 短期状态

而点赞、收藏、评论点赞这类用户行为关系，后续往往还需要支持：

- 我的点赞列表
- 我的收藏列表
- 谁点赞了我的帖子/评论
- 按时间排序
- 数据分析

这些查询都更适合数据库表来承载。

### 2.3 扩展能力弱

Redis Set 适合判断“是否点赞过”，但不适合复杂查询和统计。后续一旦要做互动中心、消息通知、用户行为分析，就会发现数据库表模型更自然。

***

## 3. 总体设计原则

建议整体调整为以下模式：

### 3.1 关系数据落表

将以下关系改为数据库表作为主存储：

- 帖子点赞关系
- 帖子收藏关系
- 评论点赞关系

### 3.2 计数字段继续冗余在主业务表

保留以下冗余计数，便于高频展示：

- `post.likes`
- `post.collections`
- `post.comments`
- `post.views`
- `comment.likes`

关系表负责“真实关系”，冗余字段负责“快速展示数量”。

### 3.3 Redis 角色降级为辅助

Redis 仍然保留，但不再承担关系主存储角色，而是用于：

- 浏览量增量聚合
- 去重防刷
- 热点缓存
- 短期状态控制

***

## 4. 点赞 / 收藏 / 评论点赞的统一设计

建议三类互动行为采用统一思路：

- 一张关系表表示某用户对某对象是否已操作
- 主表维护冗余计数
- 查询列表时批量查当前用户关系
- 查询详情时单独查当前用户关系

### 4.1 推荐表模型：逻辑状态型

推荐使用“逻辑状态型”而不是“物理删除型”。

即一条关系长期存在，通过 `status` 表示当前是否有效：

- `status = 1`：已点赞 / 已收藏
- `status = 0`：已取消

#### 优点

- 可保留行为轨迹
- 避免频繁 delete / insert
- 后续做审计、分析、消息通知更方便

#### 缺点

- 查询时需要带上 `status = 1`

综合来看，逻辑状态型更适合社区产品后续扩展。

***

## 5. 表结构设计

### 5.1 帖子点赞表 `post_like`

```sql
CREATE TABLE post_like (
  id BIGINT NOT NULL PRIMARY KEY,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_post (user_id, post_id),
  KEY idx_post_id (post_id),
  KEY idx_user_id (user_id)
);
```

### 5.2 帖子收藏表 `post_collect`

```sql
CREATE TABLE post_collect (
  id BIGINT NOT NULL PRIMARY KEY,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_post (user_id, post_id),
  KEY idx_post_id (post_id),
  KEY idx_user_id (user_id)
);
```

### 5.3 评论点赞表 `comment_like`

```sql
CREATE TABLE comment_like (
  id BIGINT NOT NULL PRIMARY KEY,
  comment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_comment (user_id, comment_id),
  KEY idx_comment_id (comment_id),
  KEY idx_user_id (user_id)
);
```

***

## 6. 帖子点赞方案

### 6.1 接口保持不变

仍使用：

- `POST /post/{postId}/like`

返回最新点赞数：

- `Result<Integer>`

### 6.2 toggleLike 核心流程

1. 查询 `post_like` 是否存在 `(user_id, post_id)`
2. 如果不存在：
   - 插入一条 `status = 1`
   - `post.likes + 1`
3. 如果存在且 `status = 0`：
   - 更新为 `status = 1`
   - `post.likes + 1`
4. 如果存在且 `status = 1`：
   - 更新为 `status = 0`
   - `post.likes - 1`

### 6.3 计数更新建议

计数更新必须使用原子 SQL：

```sql
update post set likes = likes + 1 where id = ?;
```

取消点赞时建议防止出现负数：

```sql
update post
set likes = case when likes > 0 then likes - 1 else 0 end
where id = ?;
```

### 6.4 查询点赞状态

#### 详情页

查询当前用户是否点赞：

```sql
select status from post_like where user_id = ? and post_id = ?;
```

#### 列表页

针对当前页 `postIds` 批量查：

```sql
select post_id
from post_like
where user_id = ?
  and status = 1
  and post_id in (...);
```

然后转为 `Set<Long>`，组装 `liked` 字段。

***

## 7. 帖子收藏方案

帖子收藏与帖子点赞完全平移：

- 接口：`POST /post/{postId}/collect`
- 关系表：`post_collect`
- 冗余字段：`post.collections`
- 查询方式：详情单查、列表批量查

### 7.1 toggleCollect 核心流程

1. 查询 `post_collect`
2. 不存在则插入 `status = 1`，并 `collections + 1`
3. 已存在但 `status = 0` 则更新为 `1`，并 `collections + 1`
4. 已存在且 `status = 1` 则更新为 `0`，并 `collections - 1`

### 7.2 为什么收藏也建议落表

后续收藏功能通常会衍生出：

- 我的收藏
- 收藏夹
- 按收藏时间排序
- 收藏通知

这些天然更适合数据库表模型。

***

## 8. 评论点赞方案

评论点赞和帖子点赞是同类问题，建议同步改造。

### 8.1 关系表

使用 `comment_like` 表保存：

- `comment_id`
- `user_id`
- `status`

### 8.2 冗余计数

保留 `comment.likes` 字段，用于评论列表快速展示。

### 8.3 toggleLike 流程

接口可继续保持：

- `POST /comment/{commentId}/like`

流程与帖子点赞一致：

1. 查 `comment_like`
2. 不存在则插入 `status = 1`，并 `comment.likes + 1`
3. 存在且 `status = 0`，改为 `1`，并 `comment.likes + 1`
4. 存在且 `status = 1`，改为 `0`，并 `comment.likes - 1`

### 8.4 评论列表状态查询

评论列表同样要批量查：

```sql
select comment_id
from comment_like
where user_id = ?
  and status = 1
  and comment_id in (...);
```

然后转成 `Set<Long>` 回填 `liked`。

### 8.5 为什么评论点赞也要一起改

如果只改帖子点赞/收藏，不改评论点赞，会出现两套存储模型并存：

- 帖子用表
- 评论用 Redis Set

这样后续维护成本更高，统一改成表结构更整洁。

***

## 9. 评论数方案

评论数和点赞关系不同，不需要单独关系表，因为评论本身已经有业务表。

### 9.1 设计目标

保留 `post.comments` 作为冗余字段，用于：

- 首页帖子列表展示
- 详情页展示
- 创作中心展示

### 9.2 统计口径

建议 `post.comments` 统计：

- 顶级评论 + 子评论

原因：

- 用户感知更自然
- 数据看起来更完整
- 与大部分社区平台一致

### 9.3 更新时机

#### 创建评论成功后

- `post.comments + 1`

#### 删除评论成功后

- `post.comments - 1`

### 9.4 跨服务联动建议

由于 `comment` 服务与 `post` 服务分离，推荐当前阶段采用**同步 RPC**：

- 评论创建成功后，comment 服务调用 post 服务内部接口：评论数 +1
- 评论删除成功后，comment 服务调用 post 服务内部接口：评论数 -1

建议增加内部接口，例如：

- `POST /post/internal/{postId}/comment/incr`
- `POST /post/internal/{postId}/comment/decr`

### 9.5 为什么当前推荐 RPC 而不是 MQ

MQ 更解耦，但你当前项目阶段更适合先上同步 RPC：

- 实现简单
- 易排查
- 易联调
- 对当前复杂度更友好

等后续系统稳定、消息通知和异步链路增多后，再升级为 MQ 更合适。

***

## 10. 浏览数方案

浏览数不建议每次访问详情页都直接更新数据库，因为写频率太高。

### 10.1 设计目标

- 支持高频访问
- 降低数据库写压力
- 允许轻微延迟
- 支持防刷或短期去重

### 10.2 推荐方案：Redis 聚合增量 + 定时刷库

#### 访问详情时

后端在详情接口或单独埋点接口中，对浏览数做增量记录。

推荐使用 Redis Hash 聚合：

- key：`post:view:delta`
- field：`postId`
- value：增量值

示例：

```text
HINCRBY post:view:delta {postId} 1
```

### 10.3 定时任务刷库

由定时任务周期性执行：

1. 读取 `post:view:delta`
2. 取出每个 `postId -> increment`
3. 批量更新数据库：`post.views = post.views + increment`
4. 成功后清除已处理增量

### 10.4 去重建议

浏览数如果完全不去重，同一用户频繁刷新会抬高数据。

建议做短期去重：

#### 登录用户

基于：

- `userId + postId`

#### 未登录用户

基于：

- `ip + postId`

去重时间建议：

- 30 分钟

Redis 可设置短期 key，例如：

- `post:view:dedup:{postId}:{userId}`
- `post:view:dedup:{postId}:{ip}`

存在则不记，不存在则记一次并设置 TTL。

### 10.5 为什么浏览数适合 Redis 而点赞不适合

因为浏览数本身就是：

- 高频
- 可容忍最终一致
- 无需强关系查询

而点赞/收藏/评论点赞则是：

- 用户关系数据
- 需要查是否点过
- 需要查我的点赞、我的收藏
- 更需要可持久化和可查询性

所以两者应采用不同方案。

***

## 11. 与当前项目的映射建议

### 11.1 需要改造的互动关系

建议后续统一迁移：

- 帖子点赞：Redis Set -> `post_like`
- 帖子收藏：Redis Set -> `post_collect`
- 评论点赞：Redis Set -> `comment_like`

### 11.2 需要保留的冗余字段

继续保留：

#### post 表

- `likes`
- `collections`
- `comments`
- `views`

#### comment 表

- `likes`

### 11.3 Redis 后续职责

Redis 建议保留用于：

- 浏览量增量聚合
- 浏览去重
- 热点缓存
- 限流防刷

而不再用来作为点赞/收藏/评论点赞的主存储。

***

## 12. 推荐落地顺序

### 第一阶段：改造关系数据

优先完成：

1. 帖子点赞迁表
2. 帖子收藏迁表
3. 评论点赞迁表

这是数据结构层面的根问题，越早统一越好。

### 第二阶段：补评论数联动

完成：

1. 评论新增后同步增加 `post.comments`
2. 评论删除后同步减少 `post.comments`

### 第三阶段：做浏览量

完成：

1. 详情访问计数
2. Redis 聚合增量
3. 30 分钟去重
4. 定时刷回数据库

***

## 13. 最终结论

建议将互动功能拆成两类处理：

### 13.1 强关系型互动

包括：

- 帖子点赞
- 帖子收藏
- 评论点赞

方案：

- 关系落表
- 冗余计数留在主表
- 查询时批量回填用户状态

### 13.2 高频计数型互动

包括：

- 浏览数

方案：

- Redis 聚合增量
- 定时刷库
- 短期去重

### 13.3 业务数据冗余

包括：

- 帖子评论数

方案：

- 评论服务写评论成功后，同步通知 post 服务增减冗余值

这套方案兼顾了：

- 数据持久化
- 内存成本
- 查询性能
- 后续扩展性
- 当前项目的实现复杂度

适合“次元小站”当前阶段继续向完整社区产品演进。
