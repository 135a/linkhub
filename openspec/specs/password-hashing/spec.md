# Password Hashing

## Purpose

Ensure all user passwords are stored using BCrypt hashing (work factor 10) and verified through constant-time comparison, eliminating plaintext password storage.

## Requirements

### Requirement: 用户注册时密码自动 BCrypt 哈希

系统 MUST 在用户注册时将明文密码通过 BCrypt 哈希后存储，禁止明文保存密码。

#### Scenario: 用户成功注册

- **WHEN** 用户提交注册请求，包含 `username` 和明文 `password`
- **THEN** 系统使用 BCrypt 对密码进行哈希，哈希后的密码存入 `t_user.password` 字段
- **AND** 原始明文密码不写入数据库、不写入日志

#### Scenario: BCrypt 哈希失败

- **WHEN** BCrypt 编码器因系统资源不足无法完成哈希
- **THEN** 系统返回 500 错误，不创建用户记录

### Requirement: 用户登录时 BCrypt 哈希比对

系统 MUST 在用户登录时使用 BCrypt 比对待验证密码与数据库中存储的哈希值，禁止明文比对。

#### Scenario: 密码正确登录成功

- **WHEN** 用户提交登录请求，密码与数据库中 BCrypt 哈希匹配
- **THEN** 系统返回登录成功，生成 Redis session token

#### Scenario: 密码错误登录失败

- **WHEN** 用户提交登录请求，密码与数据库中 BCrypt 哈希不匹配
- **THEN** 系统返回 "密码错误" 错误信息，不生成 session token

### Requirement: BCrypt Work Factor 配置

系统 MUST 使用 BCrypt work factor（log rounds）为 10。

#### Scenario: 密码哈希输出格式

- **WHEN** 系统对任意密码进行哈希
- **THEN** 哈希输出为 `$2a$10$` 开头的 60 字符固定长度字符串
