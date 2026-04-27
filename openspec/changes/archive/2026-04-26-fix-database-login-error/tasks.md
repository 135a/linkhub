## 1. 后端配置恢复

- [x] 1.1 使用 Git 命令检索合并之前的有效 `shardingsphere-config-dev.yaml`（原 `admin` 或 `project` 模块下）文件内容。
- [x] 1.2 将获取到的有效数据源及分片规则配置覆盖写入到 `shortlink-main/main/src/main/resources/shardingsphere-config-dev.yaml` 中，解决 `rootConfig is null` 的空指针异常。

## 2. 前端提示修复与验证

- [x] 2.1 确认和修改 `console-vue/src/api/axios.js`，保证响应拦截器在收到 `success: false` 时，只会向用户弹出纯净的 `message`（如 "系统执行出错"），而绝不会展示或拼接后端的内部错误码（例如不展示 "B000001" 等字符）。
