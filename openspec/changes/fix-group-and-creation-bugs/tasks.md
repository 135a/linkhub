## 1. 后端逻辑修复

- [x] 1.1 修改 `GroupServiceImpl` 中的 `deleteGroup` 方法，确保对 `gid` 进行非空校验，并修复导致“分组标识不能为空”报错的逻辑漏洞。
- [x] 1.2 在 `application.yaml` 的 `spring.datasource.url` 中显式添加 `characterEncoding=UTF-8` 和 `useUnicode=true`。
- [x] 1.3 检查 `UserFlowRiskControlConfiguration` 或相关 Web 配置，确保全局字符编码过滤器（CharacterEncodingFilter）已启用且强制执行 UTF-8。

## 2. 前端交互优化

- [x] 2.1 修复分组管理列表组件：在执行删除操作并收到成功响应后，立即通过 Vue 响应式操作更新本地分组列表，或手动调用列表刷新接口。
- [x] 2.2 修复短链接创建组件：在 `createShortLink` 接口成功回调中，加入关闭弹窗（Dialog/Modal）的逻辑。
- [x] 2.3 在短链接创建成功后，向父组件（或全局）发送刷新列表的信号，确保主页面及时展示新创建的短链接。
- [x] 2.4 检查前端请求拦截器，确保在请求头中正确设置字符集，并能友好处理后端返回的参数校验异常。
- [x] 2.5 重构前端分组/短链接创建逻辑，使用 `try...finally` 确保弹窗在请求结束后（无论成功失败）均能关闭，并触发页面刷新。

## 3. 布隆过滤器稳定性修复

- [x] 3.1 优化 `RBloomFilterConfiguration`，增加对现有布隆过滤器参数（Size/HashIterations）的校验逻辑。
- [x] 3.2 实现配置不匹配时的自动删除与重建逻辑。
