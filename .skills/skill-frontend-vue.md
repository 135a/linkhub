# 前端 Vue 控制台技能

## 技能描述
处理短链接管理系统的 Vue 前端控制台开发任务，包括页面开发、API 对接、路由配置、状态管理等。

## 适用场景
- Vue 前端页面开发与修改
- API 接口对接
- 路由与导航配置
- 用户登录状态管理
- 组件开发与复用
- 样式与 UI 调整

## 项目上下文

### 项目结构
```
console-vue/
├── src/
│   ├── api/              # API 接口定义
│   │   ├── axios.js      # Axios 实例与拦截器
│   │   ├── index.js      # API 统一导出
│   │   └── modules/      # API 模块
│   │       ├── group.js  # 分组相关 API
│   │       ├── smallLinkPage.js  # 短链接相关 API
│   │       └── user.js   # 用户相关 API
│   ├── assets/           # 静态资源
│   │   ├── png/          # 图片资源（浏览器图标、OS图标等）
│   │   └── svg/          # SVG 图标
│   ├── components/       # 公共组件
│   │   ├── CTable.vue    # 通用表格组件
│   │   ├── LabelSelect.vue  # 标签选择组件
│   │   └── emptyList.vue # 空列表组件
│   ├── core/
│   │   └── auth.js       # 认证工具（Token 管理）
│   ├── router/
│   │   └── index.js      # 路由配置
│   ├── store/
│   │   └── index.js      # 状态管理
│   ├── utils/
│   │   └── plugins.js    # 工具插件
│   ├── views/            # 页面视图
│   │   ├── home/         # 首页
│   │   ├── login/        # 登录页
│   │   ├── mine/         # 个人中心
│   │   ├── mySpace/      # 我的空间（主页面）
│   │   └── recycleBin/   # 回收站
│   ├── App.vue           # 根组件
│   ├── main.js           # 入口文件
│   └── style.scss        # 全局样式
├── index.html            # HTML 模板
├── vite.config.js        # Vite 配置
├── nginx.conf            # Nginx 配置
└── Dockerfile            # Docker 构建文件
```

### 核心文件说明

#### API 层
- **axios.js**: Axios 实例配置，包含请求/响应拦截器，自动附加 Token
- **modules/group.js**: 分组 CRUD 接口（新增、查询、修改、删除、排序）
- **modules/smallLinkPage.js**: 短链接接口（创建、修改、分页查询、统计等）
- **modules/user.js**: 用户接口（注册、登录、查询、修改、退出）

#### 页面视图
- **HomeIndex.vue**: 首页，展示短链接创建入口
- **LoginIndex.vue**: 登录注册页面
- **MineIndex.vue**: 个人中心，用户信息管理
- **MySpaceIndex.vue**: 核心页面（44.5KB），短链接管理主界面，包含：
  - 分组管理侧边栏
  - 短链接列表与操作
  - 短链接创建/编辑弹窗
  - 统计数据展示
- **RecycleBinIndex.vue**: 回收站页面

#### 公共组件
- **CTable.vue**: 通用表格组件，支持分页、排序
- **LabelSelect.vue**: 标签选择组件
- **emptyList.vue**: 空数据展示组件

#### 认证机制
- **core/auth.js**: Token 存储与读取
- 登录成功后将 Token 存储在 localStorage
- 请求拦截器自动从 localStorage 读取 Token 附加到 Header
- 响应拦截器处理 401 状态码，跳转登录页

### 路由结构
路由定义在 `router/index.js` 中，主要路由：
- `/login` - 登录页
- `/home` - 首页
- `/my-space` - 我的空间
- `/mine` - 个人中心
- `/recycle-bin` - 回收站

### 技术栈
- **Vue 3** + **Composition API**
- **Vite** 构建工具
- **pnpm** 包管理器
- **Axios** HTTP 客户端
- **SCSS** 样式预处理

## 代码规范

### API 模块开发
1. 在 `api/modules/` 下创建模块文件
2. 使用 `axios.js` 中导出的实例发送请求
3. 在 `api/index.js` 中统一导出
4. API 函数命名遵循 `动词 + 资源` 约定（如 `createShortLink`, `pageShortLink`）

### 页面开发
1. 在 `views/` 下创建页面目录和组件
2. 在 `router/index.js` 中注册路由
3. 需要鉴权的路由添加路由守卫
4. 复杂页面拆分为子组件放在 `components/` 子目录

### 组件开发
1. 公共组件放在 `components/` 目录
2. 使用 `defineProps` 和 `defineEmits` 定义接口
3. 组件命名使用 PascalCase
4. 样式使用 `<style lang="scss" scoped>`

### 样式规范
1. 全局样式定义在 `style.scss` 中
2. 基础样式定义在 `assets/base.css` 和 `assets/main.css` 中
3. 页面和组件使用 scoped 样式
4. 使用中文命名的图片资源（如 `创意工坊.png`、`短链默认图标.png`）

## 常见任务指南

### 新增页面
1. 在 `views/` 下创建页面目录和 `.vue` 文件
2. 在 `router/index.js` 中添加路由配置
3. 如需鉴权，添加 `meta: { requiresAuth: true }`
4. 在导航组件中添加入口

### 新增 API 接口
1. 在 `api/modules/` 对应模块中添加函数
2. 函数签名参考已有 API 风格
3. 在页面中导入并调用
4. 处理 loading 状态和错误提示

### 对接后端新接口
1. 确认后端 API 路径和参数格式
2. 在对应 API 模块中添加请求函数
3. 在页面中调用并处理响应
4. 注意 Token 自动附加机制

### 修改统计图表
1. 统计数据展示在 `MySpaceIndex.vue` 中
2. 使用图片资源展示浏览器、OS、设备等维度
3. 图片资源在 `assets/png/` 目录下
4. 新增统计维度需同步添加对应图标
