## Context

当前批量创建短链接功能存在以下现状：

- `smallLinkPage.js` 中 `addLinks` 方法设置了 `responseType: 'arraybuffer'`，导致 Axios 把后端返回的 JSON 响应当作二进制流处理，页面随即触发一次空 Excel 文件下载。
- `CreateLinks.vue` 中的 `onSubmit` 函数在收到响应后直接调用 `downLoadXls(res)`，未做正确的 JSON 响应解析。
- 描述信息完全依赖用户手动输入，虽已有注释掉的标题自动查询逻辑但未启用。
- `MySpaceIndex.vue` 的工具栏仅有「创建短链」、「批量创建」、「刷新数据」三个按钮，缺少批量操作入口。
- `<el-table>` 已有 `type="selection"` 列，但没有绑定选中数据的 ref，也没有任何批量操作逻辑与之联动。

## Goals / Non-Goals

**Goals:**
- 修复批量创建接口调用方式，使其正确接收 JSON 响应并在成功后刷新列表。
- 实现批量创建时描述信息自动从 `/title` 接口按行获取并填充，行顺序与跳转链接严格对应。
- 在主页面工具栏新增「导出 Excel」和「批量删除」两个按钮，与列表多选状态联动。
- 纯前端使用 `xlsx`（SheetJS）库生成 Excel 文件，无需后端接口。
- 批量删除复用现有 `POST /recycle-bin/save` 接口，逐条调用并汇总结果。

**Non-Goals:**
- 不修改任何后端接口（`/create/batch`、`/title`、`/recycle-bin/save` 均保持原有签名）。
- 不实现服务端批量导出能力（Excel 生成完全在浏览器端完成）。
- 不改动回收站页面的多选逻辑（本次只针对主页面短链接列表）。
- 不实现批量编辑功能。

## Decisions

### 决策 1：修复 `addLinks` 的 responseType

**方案**：删除 `smallLinkPage.js` 中 `addLinks` 方法的 `responseType: 'arraybuffer'` 配置，恢复为默认 JSON 模式。

**理由**：当前后端 `POST /create/batch` 返回的是标准 JSON，不是 Excel 文件流。`arraybuffer` 设置是历史遗留错误，去掉后 Axios 即可正常反序列化响应体。

**替代方案**：后端同时支持 JSON 和 Excel 两种响应格式（通过 Accept 头区分）→ 拒绝，引入不必要的后端改动。

---

### 决策 2：描述信息自动填充策略

**方案**：在 `CreateLinks.vue` 中监听 `formData.originUrls` 变化（使用防抖，延迟 800ms）。当输入稳定后，过滤出所有通过 URL 格式校验的行，用 `Promise.all` 并发调用 `GET /title`，按原始行索引对齐结果，写入 `formData.describes`。对调用失败或超时的行，填入空字符串占位。

**理由**：
- 并发请求比串行更快，100 行链接的填充时间取决于最慢的单次请求而非总和。
- 按行索引对齐保证顺序严格一致，避免竞态。
- 失败行填入空字符串而非跳过，保持行数与链接行数始终相等，满足后续的行数校验要求。

**替代方案**：用户失焦后才触发查询 → 防抖更自然，无需等待失焦事件。

**防抖时机**：检测用户停止输入（800ms 无新输入）后触发，避免每次按键都发出大量并发请求。

---

### 决策 3：Excel 导出——纯前端方案

**方案**：在 `MySpaceIndex.vue` 中引入 `xlsx`（SheetJS）库，将选中行的 `{ describe, fullShortUrl, originUrl, createTime }` 字段转换为工作表数据，通过 `XLSX.writeFile` 触发浏览器下载。文件名格式：`shortlinks_YYYYMMDD.xlsx`。

**理由**：
- 无需新增后端接口，实现成本最低。
- 数据量可控（单页最多 100 条），浏览器内存压力极小。
- SheetJS 是业界成熟库，已被大量 Vue 项目使用。

**前置条件**：检查项目是否已安装 `xlsx`；若未安装，任务阶段需执行 `npm install xlsx`。

**字段映射**：

| Excel 列名 | 数据字段 |
|---|---|
| 描述信息 | `describe` |
| 短链接 | `domain + '/' + shortUri` |
| 原始链接 | `originUrl` |
| 创建时间 | `createTime` |

---

### 决策 4：批量删除——串行 vs 并发

**方案**：使用 `Promise.allSettled` 并发调用 `POST /recycle-bin/save`，每条记录独立发请求（携带 `{ gid, fullShortUrl }`），全部完成后统计成功与失败数量，弹出汇总提示，并刷新列表。

**理由**：
- 现有 `/recycle-bin/save` 接口每次只接受单条记录，无批量接口，并发是最快选择。
- `Promise.allSettled` 确保部分失败不会中断其他条目的处理。
- 并发数不加限制是可接受的，短链接管理场景下选中条目通常不超过 50 条，不会对后端造成冲击。

**替代方案**：串行逐条调用 → 对于 30+ 条目延迟明显，体验较差，拒绝。

---

### 决策 5：多选状态管理

**方案**：在 `MySpaceIndex.vue` 中为 `<el-table>` 绑定 `@selection-change` 事件，将选中行存储到 `selectedRows` ref（`ref([])`）。翻页（`handleCurrentChange` / `handleSizeChange`）时清空 `selectedRows` 并调用 `tableRef.clearSelection()`。工具栏的「导出 Excel」和「批量删除」按钮根据 `selectedRows.length > 0` 控制 `disabled` 属性。

**理由**：`<el-table>` 已有 `type="selection"` 列，只需添加 `@selection-change` 事件监听即可复用现有基础设施，改动最小。

## Risks / Trade-offs

- **`/title` 接口响应速度不一致** → 防抖 + 加载状态（`isLoading`）已有实现，调用期间在描述信息框显示 loading，体验可接受。若某些链接的标题查询耗时过长，空字符串占位保证流程不阻塞。
- **xlsx 库体积** → SheetJS 压缩后约 800KB，对首屏加载有轻微影响。可通过动态导入（`import('xlsx')`）在用户点击导出时按需加载，减少初始包体积。
- **并发批量删除对后端的瞬时压力** → 在正常使用场景下（通常 < 30 条），并发请求量可接受；若未来需支持大批量场景，可在 tasks 阶段引入并发控制（每批 5 条）。

## Migration Plan

本次变更均为纯前端改动，无数据迁移需求，无需制定回滚策略。部署步骤：

1. 安装 `xlsx` 依赖（如项目尚未安装）：`npm install xlsx`
2. 修改 `smallLinkPage.js`，删除 `responseType: 'arraybuffer'`
3. 修改 `CreateLinks.vue`，实现自动填充逻辑和正确的提交后处理
4. 修改 `MySpaceIndex.vue`，添加多选状态、导出按钮、批量删除按钮
5. 本地验证批量创建、导出、批量删除三个流程
6. 重新打包并部署前端静态资源

## Open Questions

- `xlsx` 库是否已经存在于 `package.json` 的依赖中？（需在任务执行前确认，若未安装则在 task 1 中执行安装）
- 批量删除时是否需要限制单次最多可选条数（如 50 条）？当前设计无上限限制。
