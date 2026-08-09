-- ============================================================
-- Cyxz 索引优化迁移脚本
-- 日期: 2026-08-09
-- 说明: post 表用 3 个复合索引替换 4 个单列索引，覆盖高频查询
--       - idx_user_status_time:  首页/个人页帖子列表（user_id + status + 时间倒序）
--       - idx_circle_status_time: 圈子帖子列表（circle_id + status + 时间倒序）
--       - idx_user_pinned:        用户置顶帖查询（user_id + status + is_pinned + pinned_time）
-- 原 idx_user_id / idx_circle_id / idx_status / idx_create_time 被
--      复合索引最左前缀覆盖，删除避免冗余索引拖慢写入。
-- ============================================================

USE cyxz_post;

-- 1. 新增 3 个复合索引
ALTER TABLE post ADD INDEX idx_user_status_time (user_id, status, create_time);
ALTER TABLE post ADD INDEX idx_circle_status_time (circle_id, status, create_time);
ALTER TABLE post ADD INDEX idx_user_pinned (user_id, status, is_pinned, pinned_time);

-- 2. 删除被覆盖的冗余单列索引
ALTER TABLE post DROP INDEX idx_user_id;
ALTER TABLE post DROP INDEX idx_circle_id;
ALTER TABLE post DROP INDEX idx_status;
ALTER TABLE post DROP INDEX idx_create_time;

-- 3. 验证
SHOW INDEX FROM post;
