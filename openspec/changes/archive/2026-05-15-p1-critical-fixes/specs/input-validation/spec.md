## ADDED Requirements

### Requirement: Controller 层请求参数校验

系统 MUST 对所有 Controller 方法的请求体参数使用 `@Valid` 或 `@Validated` 注解进行 Bean Validation 校验。

#### Scenario: 必填字段为空时拒绝请求

- **WHEN** 用户提交创建短链接请求，`originUrl` 字段为空或 null
- **THEN** 系统返回 400 错误，错误信息包含具体字段名和校验失败原因

#### Scenario: 字段长度超限时拒绝请求

- **WHEN** 用户提交注册请求，`username` 超过最大长度限制
- **THEN** 系统返回 400 错误，错误信息包含字段名和长度限制

#### Scenario: 合法请求正常处理

- **WHEN** 用户提交创建短链接请求，所有必填字段合法
- **THEN** 系统正常处理请求，返回成功响应

### Requirement: 全局校验异常处理

系统 MUST 统一处理 `MethodArgumentNotValidException` 和 `ConstraintViolationException`，返回结构化的错误响应。

#### Scenario: @Valid 校验失败时返回统一格式

- **WHEN** 请求参数校验失败，抛出 `MethodArgumentNotValidException`
- **THEN** 系统返回包含 `code`、`message`、`data`（字段错误详情）的 JSON 响应
- **AND** HTTP 状态码为 400

#### Scenario: 单参数校验失败时返回统一格式

- **WHEN** 请求参数校验失败，抛出 `ConstraintViolationException`
- **THEN** 系统返回与 `MethodArgumentNotValidException` 相同结构的 JSON 响应

### Requirement: 请求 DTO 添加校验注解

系统 MUST 在所有请求 DTO 的关键字段上添加 Bean Validation 注解（`@NotBlank`、`@NotNull`、`@Size`、`@Email`、`@Pattern` 等）。

#### Scenario: ShortLinkCreateReqDTO 校验规则

- **WHEN** 短链接创建请求到达
- **THEN** `originUrl` 字段 MUST 非空且为合法 URL 格式
- **AND** `gid` 字段 MUST 非空

#### Scenario: UserRegisterReqDTO 校验规则

- **WHEN** 用户注册请求到达
- **THEN** `username` 字段 MUST 非空，长度 3-32 字符
- **AND** `password` 字段 MUST 非空，长度 6-64 字符

#### Scenario: UserLoginReqDTO 校验规则

- **WHEN** 用户登录请求到达
- **THEN** `username` 字段 MUST 非空
- **AND** `password` 字段 MUST 非空
