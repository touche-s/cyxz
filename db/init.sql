-- ====================================
-- Cyxz 项目数据库初始化脚本
-- ====================================

-- 用户认证表（auth 服务管理）
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY COMMENT '用户 ID（雪花算法）',
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='用户认证表';

-- 用户资料表（user 服务管理）
CREATE TABLE IF NOT EXISTS user_profile (
    user_id BIGINT PRIMARY KEY COMMENT '关联 sys_user.id',
    nickname VARCHAR(20) COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT '' COMMENT '头像 URL',
    gender TINYINT DEFAULT 0 COMMENT '0=未知 1=男 2=女',
    bio VARCHAR(200) COMMENT '个人简介',
    birthday DATETIME COMMENT '生日',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='用户资料表';
