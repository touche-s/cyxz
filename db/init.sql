-- ============================================================
-- Cyxz 项目数据库初始化脚本（全新部署用）
-- 包含 6 个微服务库 + 圈子/板块种子数据
-- ============================================================

-- ============================================================
-- 1. cyxz_auth  —— 认证服务
--    表: sys_user（用户登录凭据、角色、状态）
-- ============================================================
CREATE DATABASE IF NOT EXISTS cyxz_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE cyxz_auth;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY COMMENT '用户 ID（雪花算法）',
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=正常',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：admin/user',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='用户认证表';

-- ============================================================
-- 2. cyxz_user  —— 用户服务
--    表: user_profile（用户资料）、user_follow（关注关系）
-- ============================================================
CREATE DATABASE IF NOT EXISTS cyxz_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE cyxz_user;

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

-- ============================================================
-- 3. cyxz_post  —— 帖子服务
--    表: post（帖子）、post_like（点赞）、post_collect（收藏）
-- ============================================================
CREATE DATABASE IF NOT EXISTS cyxz_post DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE cyxz_post;

CREATE TABLE IF NOT EXISTS post (
    id BIGINT PRIMARY KEY COMMENT '帖子 ID（雪花算法）',
    user_id BIGINT NOT NULL COMMENT '作者 ID',
    circle_id BIGINT DEFAULT NULL COMMENT '圈子 ID',
    section_id BIGINT COMMENT '板块 ID',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    post_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '帖子类型：NORMAL=图文帖 ARTICLE=长文/攻略',
    content MEDIUMTEXT COMMENT '正文内容',
    cover VARCHAR(500) COMMENT '封面图 URL',
    images TEXT COMMENT '图片列表 JSON',
    tags VARCHAR(255) COMMENT '标签 JSON 数组',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=待审核 2=已通过 3=拒绝 4=已删除',
    review_reason VARCHAR(500) DEFAULT NULL COMMENT '审核拒绝原因',
    likes INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    comments INT NOT NULL DEFAULT 0 COMMENT '评论数',
    views INT NOT NULL DEFAULT 0 COMMENT '浏览数',
    collections INT NOT NULL DEFAULT 0 COMMENT '收藏数',
    is_pinned TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶 0=否 1=是',
    pinned_time DATETIME NULL COMMENT '置顶时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_circle_id (circle_id),
    INDEX idx_section_id (section_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='帖子表';

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

-- ============================================================
-- 4. cyxz_circle —— 圈子服务
--    表: circle（圈子）、circle_member（成员）、
--        section_template（板块模板，全局定义）、
--        circle_section（圈子-板块关联，每个圈启用哪些板块）
--    种子数据: 18 个圈子 + 8 个板块模板 + 默认板块关联
-- ============================================================
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

CREATE TABLE IF NOT EXISTS section_template (
    id BIGINT PRIMARY KEY COMMENT '板块模板 ID（雪花算法）',
    name VARCHAR(50) NOT NULL COMMENT '板块名称',
    applicable_type VARCHAR(16) NOT NULL DEFAULT 'ALL' COMMENT '适用内容类型：ALL/NORMAL/ARTICLE',
    description VARCHAR(200) COMMENT '模板描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值（越小越靠前）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_name (name)
) ENGINE=InnoDB COMMENT='板块模板表';

CREATE TABLE IF NOT EXISTS circle_section (
    id BIGINT PRIMARY KEY COMMENT '关联 ID（雪花算法）',
    circle_id BIGINT NOT NULL COMMENT '圈子 ID',
    template_id BIGINT NOT NULL COMMENT '板块模板 ID',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认板块：1=是 0=否',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '圈子内排序（越小越靠前）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=启用 0=禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_circle_template (circle_id, template_id),
    INDEX idx_circle_id (circle_id),
    INDEX idx_template_id (template_id)
) ENGINE=InnoDB COMMENT='圈子板块关联表';

-- ---- 圈子种子数据 ----
INSERT INTO circle (id, name, slug, intro, status, sort_order, post_count, member_count, create_time, update_time) VALUES
(1,  '原创圈',       'original',  '原创设定、OC、人设与原创作品发布区',             1, 97, 0, 0, NOW(), NOW()),
(2,  '原神',         'genshin',   '旅行者，欢迎来到提瓦特大陆',                     1, 1,  0, 0, NOW(), NOW()),
(3,  '崩坏：星穹铁道', 'starrail',  '开拓者，星穹列车前方到站',                       1, 2,  0, 0, NOW(), NOW()),
(4,  '明日方舟',      'arknights', '博士，罗德岛欢迎您',                             1, 3,  0, 0, NOW(), NOW()),
(5,  '绝区零',        'zzz',       '绳匠，欢迎来到新艾利都',                         1, 4,  0, 0, NOW(), NOW()),
(6,  '崩坏3',         'honkai3rd', '为世界上所有的美好而战',                         1, 5,  0, 0, NOW(), NOW()),
(7,  '蔚蓝档案',      'bluearchive','老师，基沃托斯欢迎您的到来',                     1, 6,  0, 0, NOW(), NOW()),
(8,  'Fate',          'fate',      '人理延续保障机关·迦勒底',                       1, 7,  0, 0, NOW(), NOW()),
(9,  '碧蓝航线',      'azurlane',  '指挥官，出击！',                                1, 8,  0, 0, NOW(), NOW()),
(10, '赛马娘',        'umamusume', '梦想在此驰骋',                                  1, 9,  0, 0, NOW(), NOW()),
(11, '东方Project',   'touhou',    '幻想乡，少女们的乐园',                           1, 10, 0, 0, NOW(), NOW()),
(12, 'MyGO',          'mygo',      '迷子でもいい、前へ進め',                         1, 11, 0, 0, NOW(), NOW()),
(13, '孤独摇滚',      'bocchi',    '社恐少女与摇滚乐队的成长物语',                     1, 12, 0, 0, NOW(), NOW()),
(14, 'Love Live!',    'lovelive',  'みんなで叶える物語',                            1, 13, 0, 0, NOW(), NOW()),
(18, '初音未来',      'miku',      '世界第一的公主殿下',                              1, 17, 0, 0, NOW(), NOW()),
(19, '洛天依',        'luotianyi', '华风夏韵，洛水天依',                              1, 18, 0, 0, NOW(), NOW()),
(20, '同人创作圈',    'fanwork',   '小众作品二创、跨作品同人内容交流区',                1, 98, 0, 0, NOW(), NOW()),
(21, '日常交流圈',    'daily',     '抽卡记录、追番碎碎念、漫展返图等二次元日常交流',      1, 99, 0, 0, NOW(), NOW());

-- ---- 板块模板种子数据 ----
INSERT INTO section_template (id, name, applicable_type, description, sort_order, create_time, update_time) VALUES
(1, '综合讨论', 'ALL',     '综合讨论区',        1, NOW(), NOW()),
(2, '同人图',   'NORMAL',  '同人绘画作品分享',   2, NOW(), NOW()),
(3, 'Cos正片',  'NORMAL',  'Cosplay正片与摄影', 3, NOW(), NOW()),
(4, '同人文',   'ARTICLE', '同人小说与文学创作',  4, NOW(), NOW()),
(5, '攻略',     'ARTICLE', '游戏攻略与机制分析',  5, NOW(), NOW()),
(6, '考据分析', 'ARTICLE', '设定考据与深度分析',  6, NOW(), NOW()),
(7, '安利推荐', 'ALL',     '作品推荐与评测',     7, NOW(), NOW()),
(8, '日常分享', 'ALL',     '水区与日常杂谈',     8, NOW(), NOW());

-- ---- 每个圈子默认启用「综合讨论」+「日常分享」 ----
INSERT INTO circle_section (id, circle_id, template_id, is_default, sort_order, status, create_time, update_time)
SELECT c.id * 100 + t.id, c.id, t.id,
       CASE WHEN t.name = '综合讨论' THEN 1 ELSE 0 END,
       t.sort_order, 1, NOW(), NOW()
FROM circle c
CROSS JOIN section_template t
WHERE t.name IN ('综合讨论', '日常分享');

-- ============================================================
-- 5. cyxz_comment —— 评论服务
--    表: comment（评论）、comment_like（评论点赞）
-- ============================================================
CREATE DATABASE IF NOT EXISTS cyxz_comment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE cyxz_comment;

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

-- ============================================================
-- 6. cyxz_message —— 消息通知服务
--    表: notification（通知）
-- ============================================================
CREATE DATABASE IF NOT EXISTS cyxz_message DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE cyxz_message;

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知 ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者用户 ID',
    sender_id BIGINT NOT NULL COMMENT '触发者用户 ID',
    type VARCHAR(32) NOT NULL COMMENT '通知类型',
    target_id BIGINT COMMENT '目标 ID（帖子 ID / 评论 ID）',
    target_type VARCHAR(32) COMMENT '目标类型: post/comment',
    related_id BIGINT COMMENT '关联 ID（评论所属帖子 ID）',
    content VARCHAR(512) COMMENT '通知摘要内容',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_receiver_read (receiver_id, is_read, create_time),
    INDEX idx_receiver_type (receiver_id, type, create_time),
    UNIQUE INDEX uk_dedup (receiver_id, sender_id, type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话 ID',
    user_id_1 BIGINT NOT NULL COMMENT '用户 ID（较小值，保证 user_id_1 < user_id_2）',
    user_id_2 BIGINT NOT NULL COMMENT '用户 ID（较大值）',
    last_message TEXT COMMENT '最后一条消息内容（冗余，会话列表预览用）',
    last_message_at DATETIME COMMENT '最后消息时间（排序用）',
    unread_count_1 INT NOT NULL DEFAULT 0 COMMENT '用户1的未读消息数',
    unread_count_2 INT NOT NULL DEFAULT 0 COMMENT '用户2的未读消息数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_users (user_id_1, user_id_2),
    INDEX idx_user1_time (user_id_1, last_message_at),
    INDEX idx_user2_time (user_id_2, last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信会话表';

CREATE TABLE IF NOT EXISTS private_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息 ID',
    conversation_id BIGINT NOT NULL COMMENT '所属会话 ID',
    sender_id BIGINT NOT NULL COMMENT '发送者用户 ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者用户 ID',
    content TEXT NOT NULL COMMENT '消息内容',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0=未读 1=已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_conversation_time (conversation_id, create_time),
    INDEX idx_receiver_read (receiver_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信消息表';
