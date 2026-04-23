-- MySQL log frontend users table
-- Used for log frontend authentication (username + bcrypt password)

CREATE DATABASE IF NOT EXISTS log_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE log_system;

CREATE TABLE IF NOT EXISTS log_users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(128) NOT NULL COMMENT 'bcrypt加密密码',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志系统用户表';

-- Login rate limit tracking table
CREATE TABLE IF NOT EXISTS login_attempts (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip_address  VARCHAR(45)  NOT NULL,
    attempt_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ip_time (ip_address, attempt_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录频率限制追踪';

-- Default admin user (password: admin123)
INSERT IGNORE INTO log_users (username, password) VALUES
('admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW');

-- Default test user (password: test123)
INSERT IGNORE INTO log_users (username, password) VALUES
('test', '$2a$10$QqfG73k4W9GQvX6Lz3vJMe3F1G7e3e7e3e7e3e7e3e7e3e7e3e7e3e7e3e7');


