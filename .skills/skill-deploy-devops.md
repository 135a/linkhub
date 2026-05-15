# 部署与运维技能

## 技能描述
处理短链接系统的部署、Docker 容器化、数据库初始化、Nginx 配置、性能压测等 DevOps 相关任务。

## 适用场景
- Docker 容器化部署
- Docker Compose 编排调整
- 数据库初始化与迁移
- Nginx 反向代理配置
- 性能压测方案设计
- 生产环境部署
- 监控与可观测性配置

## 项目上下文

### 部署相关文件
- **Docker Compose**: `shortlink-main/docker-compose.yml`
- **主服务 Dockerfile**: `shortlink-main/main/Dockerfile`
- **Vue 前端 Dockerfile**: `shortlink-main/console-vue/Dockerfile`
- **Nginx Dockerfile**: `shortlink-main/Dockerfile.nginx`
- **Nginx 配置**: `shortlink-main/console-vue/nginx.conf`
- **部署 Nginx**: `shortlink-main/deploy/nginx/`
- **部署脚本**: `shortlink-main/deploy.bat`

### 数据库相关
- **MySQL Dockerfile**: `shortlink-main/mysql/Dockerfile`
- **MySQL 初始化**: `shortlink-main/mysql/database/`
- **ClickHouse Dockerfile**: `shortlink-main/clickhouse/Dockerfile`
- **ClickHouse 初始化**: `shortlink-main/clickhouse/init/`

### 压测相关
- **压测目录**: `test/jmeter/`
- **压测说明**: `test/jmeter/README.md`
- **创建短链接压测**: `test/jmeter/create-qps-test.jmx`
- **批量创建压测**: `test/jmeter/batch-create-test.jmx`
- **重定向缓存压测**: `test/jmeter/redirect-cache-test.jmx`
- **统计并发压测**: `test/jmeter/stats-concurrency-test.jmx`
- **批量删除压测**: `test/jmeter/bulk-delete-test.jmx`

### 文档
- **架构文档**: `shortlink-main/docs/architecture.md`
- **部署文档**: `shortlink-main/docs/deployment.md`
- **可观测性文档**: `shortlink-main/docs/observability.md`
- **预演文档**: `shortlink-main/docs/preprod-drill.md`
- **割接清单**: `shortlink-main/docs/monolith-cutover-checklist.md`
- **面试题库**: `shortlink-main/docs/interview-result-pack.md`

### 性能报告
- **性能优化报告**: `docs/performance_optimization_report.md`
- **布隆过滤器验证**: `docs/bloom-filter-validation.md`
- **缓存指标验证**: `docs/cache-metrics-validation.md`
- **社区分享性能**: `docs/community_share_performance.md`
- **创建压测报告**: `docs/shortlink_create_stress_test_report.md`
- **批量创建压测报告**: `docs/shortlink_batch_create_stress_test_report.md`
- **统计压测报告**: `docs/shortlink_stats_stress_test_report.md`
- **吞吐量瓶颈报告**: `docs/bug-reports/sync-send-throughput-bottleneck.md`

### 技术栈
| 组件 | 用途 |
|------|------|
| Spring Boot | 后端主服务 |
| MySQL + ShardingSphere | 分库分片关系型数据库 |
| Redis | 缓存 + 消息队列 + 限流 |
| ClickHouse | 统计数据 OLAP 分析 |
| Sentinel | 流量控制与熔断 |
| Vue 3 + Vite | 前端管理控制台 |
| Nginx | 反向代理 + 静态资源 |
| Docker + Docker Compose | 容器化部署 |
| JMeter | 性能压测 |
| Bloom Filter | 短链接去重判断 |

### Docker Compose 服务编排
主要服务组件：
- **shortlink-main**: Spring Boot 后端服务
- **console-vue**: Vue 前端服务
- **mysql**: MySQL 数据库
- **redis**: Redis 缓存
- **clickhouse**: ClickHouse 统计数据库
- **nginx**: 反向代理

## 代码规范

### Docker 规范
- 每个服务有独立的 Dockerfile
- 使用多阶段构建优化镜像大小
- 前端构建后通过 Nginx 提供静态资源服务
- 数据库初始化脚本放在对应目录的 `init/` 或 `database/` 下

### 部署规范
- 使用 `docker-compose.yml` 统一编排
- 开发环境和生产环境使用不同的 ShardingSphere 配置
- Nginx 配置处理前后端路由和 API 代理
- 敏感配置通过环境变量注入

### 压测规范
- 使用 JMeter 进行性能压测
- 压测脚本使用 `.jmx` 格式
- 压测报告记录在 `docs/` 目录下
- 关注 QPS、响应时间、错误率等指标

## 常见任务指南

### 本地环境启动
1. 确保 Docker 和 Docker Compose 已安装
2. 执行 `docker-compose up -d` 启动所有服务
3. 等待 MySQL 和 ClickHouse 初始化完成
4. 访问前端页面验证服务状态

### 新增数据库表
1. 在 `mysql/database/` 下添加 SQL 初始化脚本
2. 如需 ClickHouse 表，在 `clickhouse/init/` 下添加
3. 修改 Dockerfile 确保新脚本被加载
4. 重建数据库容器使变更生效

### 修改 Nginx 配置
1. 修改 `console-vue/nginx.conf` 或 `deploy/nginx/` 下的配置
2. 添加新的 API 代理规则
3. 重新构建前端 Docker 镜像
4. 验证代理规则是否生效

### 性能压测
1. 安装 JMeter
2. 导入对应的 `.jmx` 压测脚本
3. 配置目标服务器地址和线程数
4. 执行压测并分析结果
5. 将压测报告保存在 `docs/` 目录下

### 生产环境部署
1. 参照 `docs/deployment.md` 部署文档
2. 修改 `shardingsphere-config-prod.yaml` 为生产配置
3. 执行 `docs/preprod-drill.md` 预演检查
4. 按照 `docs/monolith-cutover-checklist.md` 割接清单操作
5. 配置监控告警，参考 `docs/observability.md`

### 排查性能问题
1. 查看 `docs/performance_optimization_report.md` 了解已知优化点
2. 查看 `docs/bug-reports/` 下的已知问题
3. 使用 JMeter 复现性能问题
4. 关注 Redis 缓存命中率、数据库慢查询、Sentinel 限流效果
5. 使用 `CacheMonitorController` 和 `PerformanceMetricsController` 获取运行时指标
