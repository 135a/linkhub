## 1. 基础设施

- [x] 1.1 检查并补充 `index.html` 中的 viewport meta 标签
- [x] 1.2 在 `style.scss` 中添加全局响应式工具类（`.hidden-mobile`, `.visible-mobile` 等）

## 2. HomeIndex 顶栏汉堡菜单

- [x] 2.1 添加汉堡图标按钮（`@media (max-width: 767px)` 显示）
- [x] 2.2 实现点击展开下拉菜单，包含"项目首页"和"部署文档"链接
- [x] 2.3 手机端隐藏导航链接文字，用汉堡菜单替代

## 3. MySpaceIndex 表格→卡片切换

- [x] 3.1 添加 `isMobile` 响应式变量 + resize 监听（150ms 防抖）
- [x] 3.2 创建卡片布局模板（包含描述、短链接、原始链接、PV/UV/UIP、操作按钮）
- [x] 3.3 卡片布局中内联操作按钮（图表/编辑/删除/复制）
- [x] 3.4 `v-if` 根据 `isMobile` 切换 el-table 和卡片布局
- [x] 3.5 手机端按钮行精简（`.btn-text` 隐藏，保留图标）
- [x] 3.6 手机端侧边栏折叠体验打磨（toggle 按钮位置、过渡动画）

## 4. ChartsInfo 图表弹窗响应式

- [x] 4.1 将所有图表容器宽度从固定 px 改为 `width: 100%` + `max-width`
- [x] 4.2 图表容器添加 `overflow-x: auto` 兜底
- [x] 4.3 弹窗打开/窗口 resize 时调用 `echartsInstance.resize()`
- [x] 4.4 确保中国地图和世界地图在小屏上可读

## 5. MineIndex 个人信息页

- [x] 5.1 去掉 `el-descriptions__body` 的固定 `width: 500px`
- [x] 5.2 将"修改个人信息"按钮从 `position: absolute` 改为正常文档流布局

## 6. 弹窗统一适配

- [x] 6.1 为所有 el-dialog 添加 `@media (max-width: 767px)` 下 `width: 95vw` 的全局规则
- [x] 6.2 验证创建短链、批量创建、编辑短链、新建/编辑分组弹窗在手机端的表现

## 7. 收尾验证

- [ ] 7.1 Chrome DevTools 模拟 iPhone SE (375px)、iPhone 12 (390px)、iPad (768px) 走查全部页面
- [ ] 7.2 确认桌面端（1440px）布局与改动前一致，无回归
- [ ] 7.3 确认图表弹窗中所有 ECharts 实例正确 resize
