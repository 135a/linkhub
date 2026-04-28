## 1. 前置准备

- [x] 1.1 在 `console-vue` 目录下执行 `npm install xlsx`，确认 `package.json` 的 `dependencies` 中出现 `xlsx` 条目
- [ ] 1.2 确认后端 `POST /create/batch` 接口在正常调用时返回标准 JSON（可通过 Postman 或浏览器 DevTools 验证响应格式）

## 2. 修复批量创建接口调用（`smallLinkPage.js`）

- [x] 2.1 打开 `console-vue/src/api/modules/smallLinkPage.js`，删除 `addLinks` 方法中的 `responseType: 'arraybuffer'` 配置行及相关注释
- [x] 2.2 确认修改后 `addLinks` 方法仅保留 `url`、`method`、`data` 三个字段，与其他接口风格一致

## 3. 修复批量创建组件提交逻辑（`CreateLinks.vue`）

- [x] 3.1 移除 `CreateLinks.vue` 中的 `downLoadXls` 函数及其所有调用点
- [x] 3.2 修改 `onSubmit` 函数中对 `addLinks` 响应的处理逻辑：判断 `res?.data?.success === true` 时弹出「创建成功」提示并调用 `initFormData()` 重置表单；判断失败时弹出错误信息，保持弹窗打开
- [x] 3.3 确认 `emits('onSubmit', false)` 仍在成功路径中触发，以通知父组件 `MySpaceIndex.vue` 关闭对话框并刷新列表（`afterAddLink` 已实现此逻辑）

## 4. 实现描述信息自动填充（`CreateLinks.vue`）

- [x] 4.1 在 `CreateLinks.vue` 的 `<script setup>` 中引入防抖工具函数（使用 lodash 的 `debounce` 或自实现），防抖延迟设为 800ms
- [x] 4.2 修改监听 `formData.originUrls` 的 `watch`：在防抖回调中，将文本按行分割，对每行调用 URL 格式校验（复用现有 `reg` 正则），过滤出有效 URL 列表
- [x] 4.3 对有效 URL 列表使用 `Promise.all` 并发调用 `API.smallLinkPage.queryTitle({ url })`，按原始行索引对齐结果（无效行或失败行填入空字符串），将结果数组以换行符 `\n` 拼接后赋值给 `formData.describes`
- [x] 4.4 在并发调用期间将 `isLoading` 设为 `true`，全部完成后设为 `false`（描述信息框已绑定 `v-loading="isLoading"`，无需额外改动）
- [ ] 4.5 验证：输入 3 行有效链接后停止输入 0.8 秒，描述信息框自动填充 3 行对应标题；手动修改某行描述信息后，再次修改链接内容，验证已修改行在重新触发填充后被覆盖（符合预期行为）

## 5. 主页面多选状态管理（`MySpaceIndex.vue`）

- [x] 5.1 在 `MySpaceIndex.vue` 的 `<script setup>` 中新增 `const selectedRows = ref([])` 和 `const tableRef = ref()` 两个响应式引用
- [x] 5.2 为模板中的 `<el-table>` 绑定 `ref="tableRef"` 和 `@selection-change="(rows) => { selectedRows.value = rows }"`
- [x] 5.3 在 `handleSizeChange` 和 `handleCurrentChange` 函数中，翻页前调用 `tableRef.value?.clearSelection()` 清空选中状态，并重置 `selectedRows.value = []`

## 6. 新增工具栏批量操作按钮（`MySpaceIndex.vue`）

- [x] 6.1 在主页面工具栏（`v-if="!isRecycleBin"` 区域内）「批量创建」按钮之后，新增「导出 Excel」按钮，绑定 `:disabled="selectedRows.length === 0"` 和 `@click="exportExcel"`
- [x] 6.2 在「导出 Excel」按钮之后新增「批量删除」按钮，绑定 `:disabled="selectedRows.length === 0"` 和 `@click="batchDelete"`
- [x] 6.3 按钮样式与现有按钮保持一致（宽度约 110px，`margin-right: 10px`）

## 7. 实现导出 Excel 功能（`MySpaceIndex.vue`）

- [x] 7.1 在 `MySpaceIndex.vue` 顶部使用动态导入方式引入 xlsx：在 `exportExcel` 函数内 `const XLSX = await import('xlsx')` 按需加载
- [x] 7.2 实现 `exportExcel` 函数：
  - 若 `selectedRows.value.length === 0`，弹出提示「请先选择要导出的短链接」并返回
  - 将 `selectedRows.value` 映射为包含 `{ 描述信息, 短链接, 原始链接, 创建时间 }` 字段的对象数组
  - 短链接字段值拼接为 `row.domain + '/' + row.shortUri`
  - 使用 `XLSX.utils.json_to_sheet(data)` 生成工作表，`XLSX.utils.book_new()` 创建工作簿，`XLSX.utils.book_append_sheet` 附加工作表
  - 文件名格式：`shortlinks_YYYYMMDD.xlsx`（YYYYMMDD 取当前日期）
  - 调用 `XLSX.writeFile(wb, fileName)` 触发下载
- [ ] 7.3 验证：选中 2 条短链接，点击「导出 Excel」，确认浏览器下载含正确表头和数据行的 xlsx 文件；未选中时按钮禁用无法点击

## 8. 实现批量删除功能（`MySpaceIndex.vue`）

- [x] 8.1 实现 `batchDelete` 函数：首先弹出 `ElMessageBox.confirm` 确认对话框，提示「确定将选中的 X 条短链接移入回收站吗？」，用户取消则直接返回
- [x] 8.2 用户确认后，使用 `Promise.allSettled` 对 `selectedRows.value` 中每条记录并发调用 `API.smallLinkPage.toRecycleBin({ gid: row.gid, fullShortUrl: row.fullShortUrl })`
- [x] 8.3 全部请求完成后，统计 `fulfilled` 和 `rejected` 的数量；若全部成功弹出「成功移入回收站 X 条」；若部分失败弹出「成功 X 条，失败 Y 条」警告
- [x] 8.4 操作完成后调用 `getGroupInfo(queryPage)` 刷新列表，并重置 `selectedRows.value = []`（`clearSelection` 会在 `queryPage` 触发后因数据刷新自动重置，若不稳定可手动调用 `tableRef.value?.clearSelection()`）
- [ ] 8.5 验证：选中 2 条短链接，点击「批量删除」，确认对话框弹出；取消后列表无变化；确认后两条记录消失并出现在回收站

## 9. 端到端验证

- [ ] 9.1 **批量创建修复**：打开批量创建对话框，输入 2 条有效 URL，等待描述自动填充，点击「确认」，确认不再触发文件下载，列表中出现新创建的 2 条短链接
- [ ] 9.2 **导出 Excel**：在主页面勾选若干短链接，点击「导出 Excel」，验证下载文件的表头和内容正确
- [ ] 9.3 **批量删除**：勾选若干短链接，点击「批量删除」并确认，验证列表刷新、选中状态清空、被删条目出现在回收站
- [ ] 9.4 **按钮禁用状态**：未勾选任何条目时确认「导出 Excel」和「批量删除」按钮均处于禁用状态
- [ ] 9.5 **翻页清空选中**：勾选若干条目后翻页，确认复选框全部取消
