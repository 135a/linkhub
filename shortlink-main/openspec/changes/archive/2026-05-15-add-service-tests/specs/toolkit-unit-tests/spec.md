## ADDED Requirements

### Requirement: HashUtil Base62 编码测试
系统 SHALL 验证 `HashUtil.hashToBase62` 的正确性和幂等性。

#### Scenario: 相同输入产生相同输出
- **WHEN** 对同一字符串多次调用 hashToBase62
- **THEN** 每次返回相同结果

#### Scenario: 不同输入产生不同输出
- **WHEN** 对两个不同的字符串调用 hashToBase62
- **THEN** 返回不同的 Base62 编码

#### Scenario: 输出为有效 Base62 字符集
- **WHEN** 调用 hashToBase62 任意输入
- **THEN** 返回值仅包含 [0-9a-zA-Z] 字符

#### Scenario: 输出长度固定
- **WHEN** 调用 hashToBase62
- **THEN** 返回值为 6 位字符串

### Requirement: LinkUtil 工具方法测试
系统 SHALL 验证 `LinkUtil` 中关键工具方法的正确性。

#### Scenario: 获取真实 IP（有 Nginx 代理头）
- **WHEN** 请求头包含 `X-Forwarded-For`
- **THEN** 返回 X-Forwarded-For 中的第一个 IP

#### Scenario: 获取真实 IP（无代理头）
- **WHEN** 请求头不包含 `X-Forwarded-For` 或 `X-Real-IP`
- **THEN** 返回 `request.getRemoteAddr()` 的值

#### Scenario: 提取 URL 域名
- **WHEN** 输入 `https://www.example.com/path/to/page`
- **THEN** 提取出 `www.example.com`

#### Scenario: 获取操作系统检测
- **WHEN** User-Agent 为 Windows Chrome
- **THEN** 返回 "Windows"

#### Scenario: 获取浏览器检测
- **WHEN** User-Agent 包含 Chrome 标识
- **THEN** 返回 "Chrome"

#### Scenario: 获取设备类型检测
- **WHEN** User-Agent 为 Mobile Safari
- **THEN** 返回 "Mobile"

#### Scenario: 缓存有效时间计算（永久有效）
- **WHEN** validDate 为 null（永久有效）
- **THEN** 返回的缓存 TTL 为默认值

#### Scenario: 缓存有效时间计算（有时限）
- **WHEN** validDate 为未来某个时间点
- **THEN** 返回的 TTL 为 (validDate - now) 毫秒数

### Requirement: RandomGenerator 测试
系统 SHALL 验证 `RandomGenerator.generateRandomString` 的输出特性。

#### Scenario: 输出长度正确
- **WHEN** 调用 generateRandomString(8)
- **THEN** 返回长度为 8 的字符串

#### Scenario: 字符集范围内
- **WHEN** 调用 generateRandomString 多次
- **THEN** 所有字符均在 [0-9a-zA-Z] 范围内

#### Scenario: 高概率不重复
- **WHEN** 连续调用 10000 次 generateRandomString(6)
- **THEN** 至少存在 2 个不同的结果
