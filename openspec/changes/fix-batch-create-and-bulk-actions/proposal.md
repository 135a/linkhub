## Why

批量创建短链接功能存在严重缺陷：点击确认后系统直接触发 Excel 文件下载（文件内容为空），而非实际创建短链接；同时主页面缺少批量导出和批量删除能力，用户无法对多条短链接进行高效的批量操作。

## What Changes

- **修复**：`CreateLinks.vue` 中批量创建接口调用方式错误（`responseType: 'arraybuffer'` 导致响应被当成二进制流直接下载），修改为标准 JSON 响应模式，创建成功后仅弹出提示并刷新列表，不再触发文件下载。
- **修复**：描述信息目前为用户手动输入；改为用户输入跳转链接后，系统对每行链接调用现有 `/title` 接口自动获取标题并按顺序填充到描述信息文本框，用户可在此基础上手动修改；描述信息行数与链接行数始终保持一致。
- **新增**：主页面短链接列表支持多选（列表行首增加复选框）。
- **新增**：主页面工具栏新增「导出 Excel」按钮，支持将当前多选的短链接（包含短链接、原始链接、描述信息等字段）导出为 Excel 文件（纯前端生成，无需后端接口）。
- **新增**：主页面工具栏新增「批量删除」按钮，支持将当前多选的短链接批量移入回收站（复用现有 `/recycle-bin/save` 接口，循环调用或传递数组）。

## Capabilities

### New Capabilities

- `batch-create-shortlink-fix`：修复批量创建短链接的接口调用逻辑，取消错误的 Excel 下载行为，改为正确创建并刷新列表；同时实现描述信息自动从链接标题接口获取并按行填充。
- `shortlink-bulk-operations`：主页面短链接列表多选、批量导出 Excel（纯前端）、批量删除（移入回收站）三项批量操作能力。

### Modified Capabilities

- `shortlink-management`：短链接管理页面的交互行为发生规格级变更——批量创建成功后不再触发文件下载；列表新增多选状态管理；工具栏新增批量操作按钮。

## Impact

- **前端文件**：
  - `console-vue/src/views/mySpace/components/createLink/CreateLinks.vue`（批量创建逻辑修复 + 自动填充描述）
  - `console-vue/src/api/modules/smallLinkPage.js`（`addLinks` 接口去掉 `responseType: 'arraybuffer'`）
  - `console-vue/src/views/mySpace/MySpaceIndex.vue`（列表多选、导出按钮、批量删除按钮）
- **后端接口**（仅复用，无需改动）：
  - `GET /title`：查询单链接标题，批量创建时逐行调用
  - `POST /recycle-bin/save`：移入回收站，批量删除时复用
  - `POST /create/batch`：批量创建，修复调用方式后正常使用
- **依赖**：纯前端 Excel 导出使用 `xlsx`（SheetJS）库，需确认项目是否已安装；若未安装需通过 `npm install xlsx` 引入。
