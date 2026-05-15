## ADDED Requirements

### Requirement: 敏感配置通过环境变量注入

系统 MUST 支持通过环境变量注入数据库密码、AES 加密密钥和高德 API Key，禁止在配置文件中硬编码真实值。

#### Scenario: 环境变量已设置时使用环境变量值

- **WHEN** 环境变量 `SHORTLINK_DB_PASSWORD` 已设置为 `mySecretPwd`
- **AND** `SHORTLINK_AES_SECRET_KEY` 已设置为 `myAesKey2026`
- **AND** `SHORTLINK_AMAP_KEY` 已设置为 `myAmapKey`
- **THEN** 系统使用环境变量中的值连接数据库、加密列、调用高德 API

#### Scenario: 环境变量未设置时使用 dev 默认值

- **WHEN** 上述环境变量均未设置
- **THEN** 系统使用配置文件中预设的 dev 默认值（与当前硬编码值一致）

### Requirement: 提供环境变量配置模板

系统 MUST 提供 `.env.example` 文件，列出所有必需的环境变量及其说明。

#### Scenario: 新开发者接入

- **WHEN** 新开发者克隆仓库后查看 `main/.env.example` 文件
- **THEN** 文件列出所有环境变量名、用途说明和示例值
- **AND** 开发者可以复制为 `.env` 直接使用
