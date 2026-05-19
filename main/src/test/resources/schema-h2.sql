-- H2 test schema (minimal, for DAO integration tests)
-- Main tables without sharding for testing purposes

CREATE TABLE IF NOT EXISTS t_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain VARCHAR(128) NOT NULL DEFAULT '',
    short_uri VARCHAR(128) NOT NULL DEFAULT '',
    full_short_url VARCHAR(256) NOT NULL DEFAULT '',
    origin_url VARCHAR(2048) NOT NULL DEFAULT '',
    gid VARCHAR(32) NOT NULL DEFAULT 'default',
    favicon VARCHAR(512),
    created_type TINYINT DEFAULT 0,
    valid_date_type TINYINT DEFAULT 0,
    valid_date TIMESTAMP NULL DEFAULT NULL,
    describe VARCHAR(1024) DEFAULT '',
    total_pv INT DEFAULT 0,
    total_uv INT DEFAULT 0,
    total_uip INT DEFAULT 0,
    enable_status TINYINT DEFAULT 0,
    del_flag TINYINT DEFAULT 0,
    del_time BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_link_goto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gid VARCHAR(32) NOT NULL DEFAULT 'default',
    full_short_url VARCHAR(256) NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(256) NOT NULL,
    password VARCHAR(256) NOT NULL,
    real_name VARCHAR(256) DEFAULT '',
    phone VARCHAR(128) DEFAULT '',
    mail VARCHAR(256) DEFAULT '',
    del_flag TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gid VARCHAR(32) NOT NULL DEFAULT '',
    name VARCHAR(64) NOT NULL DEFAULT '',
    username VARCHAR(256) NOT NULL,
    sort_order INT DEFAULT 0,
    del_flag TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
