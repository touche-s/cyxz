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

-- ==================== 分类初始化数据 ====================
INSERT INTO category (id, name, description, sort_order, status, create_time, update_time) VALUES
(1, '动漫', '番剧安利、作品讨论、新番扫雷', 1, 1, NOW(), NOW()),
(2, '游戏', '二次元手游、单机大作、联机开黑', 2, 1, NOW(), NOW()),
(3, '绘画', '同人图、原创插画、绘画教程', 3, 1, NOW(), NOW()),
(4, 'COS', 'Cosplay正片、妆造分享、道具制作', 4, 1, NOW(), NOW()),
(5, '漫展', '漫展活动信息、返图、参展攻略', 5, 1, NOW(), NOW()),
(6, '同人', '同人文、轻小说创作、阅读推荐', 6, 1, NOW(), NOW()),
(7, '周边', '手办开箱、模型评测、周边交流', 7, 1, NOW(), NOW()),
(8, '闲聊', '水区、吐槽、二次元杂谈', 8, 1, NOW(), NOW()),
(9, '资源', '壁纸、音乐、表情包、工具资源', 9, 1, NOW(), NOW());

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
