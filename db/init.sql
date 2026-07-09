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
