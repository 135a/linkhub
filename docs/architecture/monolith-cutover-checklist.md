# 单体迁移验收与回滚清单

## 上线前验收

- [ ] Maven 编译通过：`mvn -pl project -DskipTests compile`
- [ ] 后端关键测试通过：`mvn -pl project -Dtest=ShortLinkControllerTest test`
- [ ] Compose 冒烟通过：`scripts/smoke-compose-health.ps1`
- [ ] 反向代理冒烟通过：`scripts/smoke-nginx.ps1`
- [ ] Prometheus 指标可访问：`/actuator/prometheus`
- [ ] API 基础路径确认：`/api/short-link/v1`
- [ ] 禁止项扫描通过（nacos/feign/dubbo 等）

## 上线后观察（30 分钟）

- [ ] 短链创建成功率无明显下降
- [ ] 短链跳转成功率无明显下降
- [ ] 4xx / 5xx 比例无异常升高
- [ ] P95/P99 延迟无明显劣化
- [ ] RocketMQ namesrv/broker 运行状态正常

## 回滚步骤

1. 停止新版本：`docker compose down`
2. 切回上一个稳定镜像与配置版本
3. 启动旧版本：`docker compose up -d`
4. 重新执行：
   - `scripts/smoke-compose-health.ps1`
   - `scripts/smoke-nginx.ps1`
5. 复核核心链路（创建、查询、跳转）与关键指标
