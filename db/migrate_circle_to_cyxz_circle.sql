-- ====================================
-- 圈子表从 cyxz_post 迁移到 cyxz_circle 脚本
-- 前提：cyxz-circle 服务已部署，该服务启动时会自动建表
-- ====================================

-- 1. 创建 cyxz_circle 数据库
CREATE DATABASE IF NOT EXISTS cyxz_circle DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 2. 从 cyxz_post 库导出 circle 和 circle_member 数据到 cyxz_circle
-- 注意：cyxz-circle 服务启动时已通过 DDL 自动建表，此处仅迁移数据
USE cyxz_circle;

-- 如果表不存在（服务未提前启动），手动建表
CREATE TABLE IF NOT EXISTS circle (
    id BIGINT PRIMARY KEY COMMENT '圈子 ID（雪花算法）',
    name VARCHAR(50) NOT NULL COMMENT '圈子名称',
    slug VARCHAR(50) NOT NULL COMMENT 'URL 友好标识',
    intro VARCHAR(200) COMMENT '一句话简介',
    avatar VARCHAR(500) COMMENT '头像 URL',
    cover VARCHAR(500) COMMENT '封面 URL',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值（越小越靠前）',
    post_count INT NOT NULL DEFAULT 0 COMMENT '帖子数',
    member_count INT NOT NULL DEFAULT 0 COMMENT '成员数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_slug (slug),
    INDEX idx_status_sort (status, sort_order)
) ENGINE=InnoDB COMMENT='圈子表';

CREATE TABLE IF NOT EXISTS circle_member (
    id BIGINT PRIMARY KEY COMMENT '主键（雪花算法）',
    circle_id BIGINT NOT NULL COMMENT '圈子 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=已加入 0=已退出',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_circle_user (circle_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_circle_id (circle_id)
) ENGINE=InnoDB COMMENT='圈子成员关系表';

-- 3. 复制数据（若表中已有数据则跳过）
INSERT INTO cyxz_circle.circle (id, name, slug, intro, avatar, cover, status, sort_order, post_count, member_count, create_time, update_time)
SELECT id, name, slug, intro, avatar, cover, status, sort_order, post_count, member_count, create_time, update_time
FROM cyxz_post.circle
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO cyxz_circle.circle_member (id, circle_id, user_id, status, create_time, update_time)
SELECT id, circle_id, user_id, status, create_time, update_time
FROM cyxz_post.circle_member
ON DUPLICATE KEY UPDATE circle_id = VALUES(circle_id);

-- 4. 验证数据迁移
-- SELECT COUNT(*) FROM cyxz_circle.circle;
-- SELECT COUNT(*) FROM cyxz_circle.circle_member;

-- 5. 确认迁移无误后，可执行以下命令删除 cyxz_post 中的圈子表（谨慎操作）
-- DROP TABLE IF EXISTS cyxz_post.circle;
-- DROP TABLE IF EXISTS cyxz_post.circle_member;
