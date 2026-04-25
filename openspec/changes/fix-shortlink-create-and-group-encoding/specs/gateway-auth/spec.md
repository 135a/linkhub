## ADDED Requirements

### Requirement: 网关正确转发用户身份请求头
Go 网关 **必须** 在将请求代理到后端服务时，将已验证用户的 `username`、`user-name`、`real-name` 等身份信息以 URL 编码（percent-encoding）格式写入对应请求头，确保包含非 ASCII 字符（如中文）的值能安全通过 HTTP 头传输。

#### Scenario: 中文用户名正确传递
- **WHEN** 已认证用户的用户名包含中文字符，且请求需要代理到 admin 服务
- **THEN** 网关 **必须** 对 `username` 请求头值执行 URL 编码（如 `张三` 编码为 `%E5%BC%A0%E4%B8%89`），再将编码后的值写入转发请求头

#### Scenario: 纯 ASCII 用户名无变化
- **WHEN** 用户名仅包含 ASCII 字符（如 `admin`）
- **THEN** 网关转发的请求头值 **必须** 与原始值相同，不得引入多余的编码

---

### Requirement: admin 服务请求头解码防重复解码
`UserTransmitFilter` **必须** 仅对已经过 URL 编码的请求头值执行 `URLDecoder.decode`，对于不含 `%` 字符的原始值应直接使用，不执行解码操作，以防止纯 UTF-8 字符串被错误解析。

#### Scenario: 已编码请求头正常解码
- **WHEN** 请求头中 `username` 的值包含 `%`（如 `%E5%BC%A0%E4%B8%89`）
- **THEN** `UserTransmitFilter` **必须** 调用 `URLDecoder.decode` 解码，将解码后的明文（如 `张三`）写入 `UserContext`

#### Scenario: 未编码请求头直接使用
- **WHEN** 请求头中 `username` 的值不含 `%`（如 `admin`）
- **THEN** `UserTransmitFilter` **必须** 直接使用原始值，**不得** 执行解码操作

---

### Requirement: 中文分组名在数据库中正确存储
MySQL 数据库中的分组表（`t_group_*`）**必须** 使用 `utf8mb4` 字符集和 `utf8mb4_unicode_ci` 排序规则，确保中文分组名在写入和读取时均不发生乱码。

#### Scenario: 中文分组名写入后读取一致
- **WHEN** 用户创建名称为中文（如 `默认分组`）的分组
- **THEN** 从数据库查询该分组时，`name` 字段值 **必须** 与写入时完全一致，不得出现乱码或字符丢失
