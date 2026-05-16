## 1. 删除冗余配置

- [x] 1.1 删除 `main/src/main/java/com/nym/shortlink/core/config/DataBaseConfiguration.java`

## 2. 验证

- [x] 2.1 编译项目：`mvn compile -pl main`
- [x] 2.2 运行全部测试：`mvn test -pl main`，确认 102 个测试通过，0 失败
