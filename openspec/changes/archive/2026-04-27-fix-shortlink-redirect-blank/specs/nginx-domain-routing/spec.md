## ADDED Requirements

### Requirement: Nginx 必须转发根路径短链接请求至后端
Nginx MUST 能够识别并转发根路径下的短链接访问请求（通常为 6 位 Base62 字符）到后端跳转接口，确保跳转逻辑被正确执行。

#### Scenario: 短链接请求正常转发
- **WHEN** 用户访问 `domain.com/xxxxxx`（其中 xxxxxx 为 6 位字母数字组合）
- **THEN** Nginx SHALL 将该请求转发至后端服务，且不添加 `/api` 前缀
