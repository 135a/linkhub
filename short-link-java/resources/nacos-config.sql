-- Nacos 配置数据库初始化脚本
CREATE DATABASE IF NOT EXISTS nacos_config CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nacos_config;

CREATE TABLE IF NOT EXISTS config_info (
  id bigint NOT NULL AUTO_INCREMENT,
  data_id varchar(255) NOT NULL,
  group_id varchar(128) DEFAULT NULL,
  content longtext NOT NULL,
  md5 varchar(32) DEFAULT NULL,
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  src_user text,
  src_ip varchar(50) DEFAULT NULL,
  app_name varchar(128) DEFAULT NULL,
  tenant_id varchar(128) DEFAULT '',
  c_desc varchar(256) DEFAULT NULL,
  c_use varchar(64) DEFAULT NULL,
  effect varchar(64) DEFAULT NULL,
  type varchar(64) DEFAULT NULL,
  c_schema text,
  encrypted_data_key text NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_configinfo_datagrouptenant (data_id,group_id,tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS config_info_aggr (
  id bigint NOT NULL AUTO_INCREMENT,
  data_id varchar(255) NOT NULL,
  group_id varchar(128) NOT NULL,
  datum_id varchar(255) NOT NULL,
  content longtext NOT NULL,
  gmt_modified datetime NOT NULL,
  app_name varchar(128) DEFAULT NULL,
  tenant_id varchar(128) DEFAULT '',
  PRIMARY KEY (id),
  UNIQUE KEY uk_configinfoaggr_datagrouptenantdatum (data_id,group_id,tenant_id,datum_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS config_info_beta (
  id bigint NOT NULL AUTO_INCREMENT,
  data_id varchar(255) NOT NULL,
  group_id varchar(128) NOT NULL,
  app_name varchar(128) DEFAULT NULL,
  content longtext NOT NULL,
  beta_ips varchar(1024) DEFAULT NULL,
  md5 varchar(32) DEFAULT NULL,
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  src_user text,
  src_ip varchar(50) DEFAULT NULL,
  tenant_id varchar(128) DEFAULT '',
  encrypted_data_key text NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_configinfobeta_datagrouptenant (data_id,group_id,tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS config_info_tag (
  id bigint NOT NULL AUTO_INCREMENT,
  data_id varchar(255) NOT NULL,
  group_id varchar(128) NOT NULL,
  tenant_id varchar(128) DEFAULT '',
  tag_id varchar(128) NOT NULL,
  app_name varchar(128) DEFAULT NULL,
  content longtext NOT NULL,
  md5 varchar(32) DEFAULT NULL,
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  src_user text,
  src_ip varchar(50) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_configinfotag_datagrouptenanttag (data_id,group_id,tenant_id,tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS config_tags_relation (
  id bigint NOT NULL,
  tag_name varchar(128) NOT NULL,
  tag_type varchar(64) DEFAULT NULL,
  data_id varchar(255) NOT NULL,
  group_id varchar(128) NOT NULL,
  tenant_id varchar(128) DEFAULT '',
  nid bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (nid),
  UNIQUE KEY uk_configtagrelation_configidtag (id,tag_name,tag_type),
  KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS group_capacity (
  id bigint NOT NULL AUTO_INCREMENT,
  group_id varchar(128) NOT NULL DEFAULT '',
  quota int unsigned NOT NULL DEFAULT '0',
  `usage` int unsigned NOT NULL DEFAULT '0',
  max_size int unsigned NOT NULL DEFAULT '0',
  max_aggr_count int unsigned NOT NULL DEFAULT '0',
  max_aggr_size int unsigned NOT NULL DEFAULT '0',
  max_history_count int unsigned NOT NULL DEFAULT '0',
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS his_config_info (
  id bigint NOT NULL,
  nid bigint NOT NULL AUTO_INCREMENT,
  data_id varchar(255) NOT NULL,
  group_id varchar(128) NOT NULL,
  app_name varchar(128) DEFAULT NULL,
  content longtext NOT NULL,
  md5 varchar(32) DEFAULT NULL,
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  src_user text,
  src_ip varchar(50) DEFAULT NULL,
  op_type char(10) DEFAULT NULL,
  tenant_id varchar(128) DEFAULT '',
  encrypted_data_key text NOT NULL,
  PRIMARY KEY (nid),
  KEY idx_gmt_create (gmt_create),
  KEY idx_gmt_modified (gmt_modified),
  KEY idx_did (data_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_capacity (
  id bigint NOT NULL AUTO_INCREMENT,
  tenant_id varchar(128) NOT NULL DEFAULT '',
  quota int unsigned NOT NULL DEFAULT '0',
  `usage` int unsigned NOT NULL DEFAULT '0',
  max_size int unsigned NOT NULL DEFAULT '0',
  max_aggr_count int unsigned NOT NULL DEFAULT '0',
  max_aggr_size int unsigned NOT NULL DEFAULT '0',
  max_history_count int unsigned NOT NULL DEFAULT '0',
  gmt_create datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  gmt_modified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_info (
  id bigint NOT NULL AUTO_INCREMENT,
  kp varchar(128) NOT NULL,
  tenant_id varchar(128) DEFAULT '',
  tenant_name varchar(128) DEFAULT '',
  tenant_desc varchar(256) DEFAULT NULL,
  create_source varchar(32) DEFAULT NULL,
  gmt_create bigint NOT NULL,
  gmt_modified bigint NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_info_kptenantid (kp,tenant_id),
  KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
  username varchar(50) NOT NULL PRIMARY KEY,
  password varchar(500) NOT NULL,
  enabled boolean NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles (
  username varchar(50) NOT NULL,
  role varchar(50) NOT NULL,
  UNIQUE INDEX idx_user_role (username,role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permissions (
  role varchar(50) NOT NULL,
  resource varchar(255) NOT NULL,
  action varchar(8) NOT NULL,
  UNIQUE INDEX uk_role_permission (role,resource,action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO users (username, password, enabled) VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', TRUE);
INSERT INTO roles (username, role) VALUES ('nacos', 'ROLE_ADMIN');
