## Why

当前系统在登录等接口调用失败时存在两方面严重问题：
1. **后端数据库配置缺失**：由于近期可能进行了模块合并或配置调整，导致 ShardingSphere 的数据源配置丢失或解析失败，引发 `rootConfig is null` 的空指针异常，致使应用无法获取数据库连接。
2. **前端错误提示不友好**：当后端发生如上所述的全局异常时，向前端返回了 `code: B000001` 且 `success: false` 的响应。前端目前展示的错误可能包含了晦涩的业务错误码。我们需要遵循更好的用户体验规范：在展示执行失败的提示时，只需展示失败原因（即 `message` 字段），无需向最终用户展示具体的业务错误码。

## What Changes

- **后端数据源配置修复**：排查并修复后端应用配置文件（如 `project_app.yaml` 等）中关于 ShardingSphere 的数据源及规则配置，解决初始化失败导致无法连接数据库的问题。
- **前端全局异常提示优化**：调整前端应用的 axios 响应拦截器或全局异常处理逻辑，在触发错误弹窗时仅提取并展示响应中的 `message` 字段，确保对用户隐藏 `B000001` 这类内部业务错误码。

## Capabilities

### New Capabilities
- `frontend-error-display`: 前端应用全局异常提示规范。定义网络或业务请求失败时，展示给用户错误信息的内容与格式约束（隐藏内部错误码，仅展示具体原因 message）。
- `database-shardingsphere-config`: 后端 ShardingSphere 数据源初始化与配置规范，确保应用具备正确且可用的数据库连接池配置。

### Modified Capabilities
- 无

## Impact

- **前端系统**：`console-vue/src/api/axios.js` 中的全局错误拦截处理。
- **后端系统**：应用的 YAML 配置文件（主要涉及 ShardingSphere 数据源初始化的配置段落）。
