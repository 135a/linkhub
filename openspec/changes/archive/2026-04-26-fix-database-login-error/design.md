## Context

当前在 `shortlink-main` 合并为单体架构（`main` 模块）之后，原有的 `shardingsphere-config-dev.yaml` 配置文件在迁移过程中变成了空文件，这直接导致应用在启动和执行数据库操作（例如用户登录）时，ShardingSphere 无法解析出数据源（`rootConfig is null`），进而抛出 `NullPointerException` 及 `MyBatisSystemException`。这使得后端的异常处理 fallback 到了默认的 `B000001` (“系统执行出错”) 响应。
另外，关于前端向用户展示的错误提示，我们必须确保只展示用户易于理解的文字信息（即响应体中的 `message` 字段），禁止将晦涩的内部业务状态码（如 `B000001`）展示到用户界面上。

## Goals / Non-Goals

**Goals:**
- 从 Git 历史中找回并恢复原 `project` 和 `admin` 模块使用的 ShardingSphere 数据源和分片配置，将其正确写入到 `main/src/main/resources/shardingsphere-config-dev.yaml`（及 prod 等环境配置）中。
- 确保系统在数据库连接正常后，登录功能能够顺利执行，并且在遇到正常的业务校验失败时返回明确的业务消息，而不是底层的系统级错误。
- 明确并保障前端拦截器展示错误时仅展示 `message`。

**Non-Goals:**
- 不涉及数据库表结构的改变，不引入新的数据源。
- 不修改原有的 Mybatis-plus 或 ShardingSphere 版本及核心执行引擎。

## Decisions

1. **后端数据库配置恢复**:
   - **Decision**: 检索并恢复遗失的 `shardingsphere-config-dev.yaml`。因为当前所有请求入口都在 `main` 模块下，统一的一份配置即可涵盖 `t_link`, `t_group` 等表的分片规则（如果使用了分库分表）以及基础的数据源 `ds_0` 配置。
   - **Rationale**: 缺少该配置会导致核心 ORM 框架瘫痪。只需恢复合并前的配置内容即可解决该空指针异常。

2. **前端错误提示纯净显示**:
   - **Decision**: 确认 `axios.js` 响应拦截器在收到 `success: false` 时，仅仅调用 `ElMessage.error(res.data.message)` 进行提示，而不进行形如 `res.data.code + ": " + res.data.message` 的拼接展示。
   - **Rationale**: 用户反馈明确指出不展示业务错误码，这有助于终端用户体验并防止内部系统实现细节泄露。

## Risks / Trade-offs

- **[Risk]** 如果原 `admin` 和 `project` 使用了不同且冲突的分库分表策略，直接合并可能需要调整。
  **[Mitigation]** 检查恢复出来的配置文件，通过比对确认配置能涵盖所需的数据源及所有规则。若原先并没有复杂的冲突规则，可直接合并。
