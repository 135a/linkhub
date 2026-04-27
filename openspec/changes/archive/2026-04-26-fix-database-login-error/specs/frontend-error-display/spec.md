# Capability: Frontend Error Display

## Purpose
前端应用全局异常提示规范。定义网络或业务请求失败时，展示给用户错误信息的内容与格式约束（隐藏内部错误码，仅展示具体原因 message）。

## ADDED Requirements

### Requirement: 全局错误提示仅展示业务消息
前端应用在进行网络响应拦截时，MUST 对业务异常（如后端返回 `success: false`）统一提取其 `message` 字段进行向用户的展示（如使用 `ElMessage.error`），并禁止在界面可见区域将具体的业务错误码（如 `B000001`）与提示文字拼接展示给用户。

#### Scenario: 收到业务失败响应
- **WHEN** 前端应用拦截到 HTTP 请求成功，但返回数据的数据体中明确表示操作失败（`success: false`）且携带了 `message` 时
- **THEN** 前端界面 MUST 通过全局 UI 提示（例如 Toast/Message 组件）仅显示该业务消息（如 "系统执行出错"），且不得夹带形如 "B000001" 等开发内部使用的错误码。
