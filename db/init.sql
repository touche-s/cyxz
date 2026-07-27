-- ====================================
-- Cyxz 项目数据库初始化脚本
-- auth 服务和 user 服务使用独立数据库
-- ====================================

-- ==================== auth 库 ====================
CREATE DATABASE IF NOT EXISTS cyxz_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE cyxz_auth;

-- 用户认证表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY COMMENT '用户 ID（雪花算法）',
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=正常',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='用户认证表';

-- ==================== user 库 ====================
CREATE DATABASE IF NOT EXISTS cyxz_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE cyxz_user;

-- 用户资料表
CREATE TABLE IF NOT EXISTS user_profile (
    user_id BIGINT PRIMARY KEY COMMENT '关联 sys_user.id',
    nickname VARCHAR(20) COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT '' COMMENT '头像 URL',
    gender TINYINT DEFAULT 0 COMMENT '0=未知 1=男 2=女',
    bio VARCHAR(200) COMMENT '个人简介',
    birthday DATE COMMENT '生日',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='用户资料表';

-- 用户关注关系表
CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT PRIMARY KEY COMMENT '主键（雪花算法）',
    user_id BIGINT NOT NULL COMMENT '关注者 ID',
    follow_user_id BIGINT NOT NULL COMMENT '被关注者 ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=已关注 0=已取消',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_user_follow (user_id, follow_user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_follow_user_id (follow_user_id)
) ENGINE=InnoDB COMMENT='用户关注关系表';

-- ==================== post 库 ====================
CREATE DATABASE IF NOT EXISTS cyxz_post DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE cyxz_post;

-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY COMMENT '分类 ID（雪花算法）',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(200) COMMENT '分类描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值（越小越靠前）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_name (name)
) ENGINE=InnoDB COMMENT='分类表';

-- 帖子表
CREATE TABLE IF NOT EXISTS post (
    id BIGINT PRIMARY KEY COMMENT '帖子 ID（雪花算法）',
    user_id BIGINT NOT NULL COMMENT '作者 ID',
    category_id BIGINT COMMENT '分类 ID',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content TEXT COMMENT '正文内容',
    cover VARCHAR(500) COMMENT '封面图 URL',
    images TEXT COMMENT '图片列表 JSON',
    tags VARCHAR(255) COMMENT '标签 JSON 数组',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=已发布 2=已删除',
    likes INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    comments INT NOT NULL DEFAULT 0 COMMENT '评论数',
    views INT NOT NULL DEFAULT 0 COMMENT '浏览数',
    collections INT NOT NULL DEFAULT 0 COMMENT '收藏数',
    is_pinned TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶 0=否 1=是',
    pinned_time DATETIME NULL COMMENT '置顶时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='帖子表';

-- 帖子点赞关系表
CREATE TABLE IF NOT EXISTS post_like (
    id BIGINT PRIMARY KEY COMMENT '主键（雪花算法）',
    post_id BIGINT NOT NULL COMMENT '帖子 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=已点赞 0=已取消',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_user_post (user_id, post_id),
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='帖子点赞关系表';

-- 帖子收藏关系表
CREATE TABLE IF NOT EXISTS post_collect (
    id BIGINT PRIMARY KEY COMMENT '主键（雪花算法）',
    post_id BIGINT NOT NULL COMMENT '帖子 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=已收藏 0=已取消',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_user_post (user_id, post_id),
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='帖子收藏关系表';

-- post 表新增圈子ID字段
ALTER TABLE post ADD COLUMN circle_id BIGINT DEFAULT NULL COMMENT '圈子 ID' AFTER category_id;
ALTER TABLE post ADD INDEX idx_circle_id (circle_id);

-- ==================== 分类初始化数据 ====================
INSERT INTO category (id, name, description, sort_order, status, create_time, update_time) VALUES
(1, '同人创作', '同人图、同人文、轻小说等二次创作', 1, 1, NOW(), NOW()),
(2, '作品讨论', '番剧、游戏、轻小说等作品深度讨论', 2, 1, NOW(), NOW()),
(3, '安利推荐', '发现好作品，分享你的心头好', 3, 1, NOW(), NOW()),
(4, '攻略考据', '游戏攻略、设定考据、深度分析', 4, 1, NOW(), NOW()),
(5, 'COS摄影', 'Cosplay正片、妆造分享、摄影作品', 5, 1, NOW(), NOW()),
(6, '周边晒单', '手办开箱、模型评测、周边交流', 6, 1, NOW(), NOW()),
(7, '日常分享', '水区、吐槽、二次元日常杂谈', 7, 1, NOW(), NOW());

-- 老帖子迁移到日常交流圈
UPDATE post SET circle_id = 21 WHERE circle_id IS NULL;

-- ==================== circle 库 ====================
CREATE DATABASE IF NOT EXISTS cyxz_circle DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE cyxz_circle;

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

INSERT INTO circle (id, name, slug, intro, status, sort_order, post_count, member_count, create_time, update_time) VALUES
(2, '原神', 'genshin', '旅行者，欢迎来到提瓦特大陆', 1, 1, 0, 0, NOW(), NOW()),
(3, '崩坏：星穹铁道', 'starrail', '开拓者，星穹列车前方到站', 1, 2, 0, 0, NOW(), NOW()),
(4, '明日方舟', 'arknights', '博士，罗德岛欢迎您', 1, 3, 0, 0, NOW(), NOW()),
(5, '绝区零', 'zzz', '绳匠，欢迎来到新艾利都', 1, 4, 0, 0, NOW(), NOW()),
(6, '崩坏3', 'honkai3rd', '为世界上所有的美好而战', 1, 5, 0, 0, NOW(), NOW()),
(7, '蔚蓝档案', 'bluearchive', '老师，基沃托斯欢迎您的到来', 1, 6, 0, 0, NOW(), NOW()),
(8, 'Fate', 'fate', '人理延续保障机关·迦勒底', 1, 7, 0, 0, NOW(), NOW()),
(9, '碧蓝航线', 'azurlane', '指挥官，出击！', 1, 8, 0, 0, NOW(), NOW()),
(10, '赛马娘', 'umamusume', '梦想在此驰骋', 1, 9, 0, 0, NOW(), NOW()),
(11, '东方Project', 'touhou', '幻想乡，少女们的乐园', 1, 10, 0, 0, NOW(), NOW()),
(12, 'MyGO', 'mygo', '迷子でもいい、前へ進め', 1, 11, 0, 0, NOW(), NOW()),
(13, '孤独摇滚', 'bocchi', '社恐少女与摇滚乐队的成长物语', 1, 12, 0, 0, NOW(), NOW()),
(14, 'Love Live!', 'lovelive', 'みんなで叶える物語', 1, 13, 0, 0, NOW(), NOW()),
(18, '初音未来', 'miku', '世界第一的公主殿下', 1, 17, 0, 0, NOW(), NOW()),
(19, '洛天依', 'luotianyi', '华风夏韵，洛水天依', 1, 18, 0, 0, NOW(), NOW()),
(1, '原创圈', 'original', '原创设定、OC、人设与原创作品发布区', 1, 97, 0, 0, NOW(), NOW()),
(20, '同人创作圈', 'fanwork', '小众作品二创、跨作品同人内容交流区', 1, 98, 0, 0, NOW(), NOW()),
(21, '日常交流圈', 'daily', '抽卡记录、追番碎碎念、漫展返图等二次元日常交流', 1, 99, 0, 0, NOW(), NOW());

-- ==================== comment 库 ====================
CREATE DATABASE IF NOT EXISTS cyxz_comment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE cyxz_comment;

-- 评论表
CREATE TABLE IF NOT EXISTS comment (
    id BIGINT PRIMARY KEY COMMENT '评论 ID（雪花算法）',
    post_id BIGINT NOT NULL COMMENT '帖子 ID',
    user_id BIGINT NOT NULL COMMENT '评论用户 ID',
    content TEXT NOT NULL COMMENT '评论内容',
    parent_id BIGINT DEFAULT NULL COMMENT '父评论 ID（null 表示顶级评论）',
    reply_to_user_id BIGINT DEFAULT NULL COMMENT '被回复用户 ID',
    post_author_id BIGINT DEFAULT NULL COMMENT '帖子作者 ID（冗余字段，用于查询用户收到的评论）',
    likes INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0=已删除 1=正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_post_author_id (post_author_id),
    INDEX idx_reply_to_user_id (reply_to_user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='评论表';

-- 评论点赞关系表
CREATE TABLE IF NOT EXISTS comment_like (
    id BIGINT PRIMARY KEY COMMENT '主键（雪花算法）',
    comment_id BIGINT NOT NULL COMMENT '评论 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=已点赞 0=已取消',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_user_comment (user_id, comment_id),
    INDEX idx_comment_id (comment_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='评论点赞关系表';

-- ==================== message 库 ====================
CREATE DATABASE IF NOT EXISTS cyxz_message DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE cyxz_message;

-- 通知表
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知 ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者用户 ID',
    sender_id BIGINT NOT NULL COMMENT '触发者用户 ID',
    type VARCHAR(32) NOT NULL COMMENT '通知类型: POST_LIKED/POST_COMMENTED/COMMENT_REPLIED/POST_COLLECTED/USER_FOLLOWED',
    target_id BIGINT COMMENT '目标 ID（帖子 ID / 评论 ID）',
    target_type VARCHAR(32) COMMENT '目标类型: post/comment',
    related_id BIGINT COMMENT '关联 ID（评论所属帖子 ID）',
    content VARCHAR(512) COMMENT '通知摘要内容（评论/回复内容截断）',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_receiver_read (receiver_id, is_read, create_time),
    INDEX idx_receiver_type (receiver_id, type, create_time),
    UNIQUE INDEX uk_dedup (receiver_id, sender_id, type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
