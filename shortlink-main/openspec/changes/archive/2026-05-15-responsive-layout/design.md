## Context

项目采用 Vue 3 + Vite + Element Plus 技术栈，前端当前全部为桌面端硬编码布局（固定 px 宽度）。上一轮探索已确认：主页面表格最小需要 1310px 宽度、图表弹窗硬编码 800px、个人信息页绝对定位。需要在**不引入新 UI 框架**的前提下，利用 Element Plus 现有组件和 CSS 媒体查询实现响应式。

目标设备断点：
- 桌面端：≥ 768px（保持现有布局不变）
- 手机端：< 768px（启用卡片布局等替代方案）
- 平板端：768px - 1024px（侧栏收窄 + 按钮文字精简，已有部分实现）

## Goals / Non-Goals

**Goals:**
- 手机端主页面可用（查看短链列表、创建短链、查看图表）
- 图表弹窗在手机端完整展示不溢出
- 导航和操作入口在手机端可达
- 所有弹窗/对话框在手机端全宽展示
- 登录页在各尺寸下美观

**Non-Goals:**
- 不引入新的 UI 框架或组件库
- 不修改后端代码
- 不添加 PWA/Service Worker
- 不做平板专属的复杂布局（已有雏形，本次仅修 bug）
- 不做暗色模式

## Decisions

### 1. 表格响应式：CSS 媒体查询 + v-if 卡片/表格切换

**选择**：用 `const isMobile = ref(window.innerWidth < 768)` + `@media (max-width: 767px)` 双重控制。

**为什么不用 el-table 自带响应式**：Element Plus 的 el-table 不支持列隐藏优先级，8 列在小屏上即使精简也放不下。不如直接切到卡片布局。

**卡片设计**：每条短链一张卡片，纵向排列，包含：描述（标题）、短链接（可点击跳转）、原始链接、今日/累计 PV/UV/UIP、操作按钮（复制/图表/编辑/删除）。

**替代方案考虑**：
- 水平滚动表格：体验差，面试官可能不知道可以横滑
- 列优先级隐藏：Element Plus 不原生支持，且 8 列即使隐到 4 列也需要 600px+

### 2. 屏幕宽度检测：resize 监听 + 防抖

**选择**：`onMounted` 中注册 `window.addEventListener('resize', ...)`，用 150ms 防抖更新 `isMobile` ref。

**为什么不只用 CSS**：表格和卡片是不同的 DOM 结构，纯 CSS 无法切换。CSS 媒体查询控制样式细节（如卡片内布局），JS 控制用哪个组件。

### 3. 图表响应式：百分比宽度 + ECharts resize

**选择**：将 ChartsInfo.vue 中所有固定宽度（800px/600px/400px/330px）改为 `width: 100%; max-width: 800px`。同时在 `isVisible()` 和窗口 resize 时调用 `echartsInstance.resize()`。

**为什么不自己写 canvas**：项目已依赖 ECharts，resize 是其内置能力，只需正确调用即可。

### 4. 汉堡菜单：纯 CSS + v-if 实现

**选择**：在 HomeIndex.vue 顶栏右侧添加汉堡图标（三条横线），点击展开下拉菜单，包含"项目首页"和"部署文档"链接。使用 el-popover 或手写 dropdown。

**替代方案**：el-drawer 从左侧滑出 → 过度设计，只有两个链接不值得。

### 5. 按钮精简：CSS 类控制文字显隐

**选择**：给按钮文字加 `.btn-text` 类，在 `@media (max-width: 767px)` 下 `display: none`。按钮保留图标（已有图标的使用图标，没有的用 el-icon 补充）。MySpaceIndex 中已部分实现此模式（第 1381 行 `.buttons-box .btn-text { display: none; }`），本次扩展覆盖所有按钮。

## Risks / Trade-offs

- **卡片视图性能**：如果分组有 100+ 条短链，卡片列表可能很长 → 分页器本身已限制每页数量，卡片也走分页，不影响
- **图表在小屏上的可读性**：中国地图在 300px 宽度下可能太小 → 设 `min-width: 300px` 并允许水平滚动，图表区域用 `overflow-x: auto`
- **Vanta.js 动态背景**：登录页的 Vanta Waves 在低端手机上可能卡顿 → 在 `max-width: 480px` 时降低 `scale` 参数或关闭动画（不影响功能）

## Open Questions

- 是否需要为平板端（768px-1024px）做更多适配？当前代码已有部分实现，本次先聚焦手机端
- 回收站页面（RecycleBinIndex）目前是空壳，后续是否有功能？
