## MODIFIED Requirements

### Requirement: 创建短链接后的交互闭环
前端在成功调用批量创建短链接接口（`POST /create/batch`）后，必须自动完成后续的 UI 状态清理工作；批量创建成功后 SHALL 仅弹出成功提示并触发列表刷新，不得触发任何文件下载行为。

#### Scenario: 批量短链接创建成功后关闭弹窗并刷新列表
- **WHEN** 前端收到 `POST /create/batch` 返回的创建成功响应（`success: true`）
- **THEN** 前端批量创建对话框 SHALL 立即关闭，主页面列表 SHALL 自动触发数据刷新以展示新生成的短链接，不触发文件下载

#### Scenario: 短链接创建成功后自动关闭弹窗
- **WHEN** 收到后端返回的单条创建成功响应
- **THEN** 前端创建对话框应立即关闭，主页面列表应自动触发数据刷新以展示新生成的短链接
