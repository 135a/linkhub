## Why

前端目前只为桌面端（≥1024px）设计，在手机浏览器上表格被截断、图表溢出、按钮排列混乱，无法正常使用。这是面试展示项目，面试官随时可能用手机打开，糟糕的移动端体验会严重影响印象分。

## What Changes

- 主页面表格在手机端切换为卡片布局，替代当前的 8 列 el-table
- 图表弹窗中的 ECharts 图表宽度从固定 px 改为响应式百分比
- 顶栏导航链接在手机端收入汉堡菜单
- 按钮行在手机端隐藏文字、只显示图标
- 侧边栏折叠机制打磨（已有雏形，完善交互细节）
- 所有弹窗在手机端全宽显示
- 个人信息页（MineIndex）去掉固定宽度和绝对定位
- 添加 viewport meta 标签（确认并补充）

## Capabilities

### New Capabilities

- `responsive-layout`: 全站响应式布局，支持手机端（<768px）和平板端（768px-1024px）正常浏览和操作，包括表格→卡片切换、图表响应式、导航折叠、弹窗适配

### Modified Capabilities

<!-- 无现有 spec 需要修改 -->

## Impact

- `console-vue/src/views/mySpace/MySpaceIndex.vue` — 核心改动：表格卡片切换、按钮适配
- `console-vue/src/views/mySpace/components/chartsInfo/ChartsInfo.vue` — 图表宽度响应式
- `console-vue/src/views/home/HomeIndex.vue` — 顶栏加汉堡菜单
- `console-vue/src/views/mine/MineIndex.vue` — 去掉固定宽度和绝对定位
- `console-vue/src/components/CTable.vue` — 高度响应式
- `console-vue/src/views/login/LoginIndex.vue` — 已有基础响应式，可能需要微调
- `console-vue/index.html` — 确认 viewport meta 标签
- `console-vue/src/style.scss` — 可能需要补充全局响应式工具类
