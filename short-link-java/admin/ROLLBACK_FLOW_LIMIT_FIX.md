# Admin 登录限流修复回滚手册

适用场景：上线 `UserFlowRiskControlFilter` 匿名限流键隔离修复后出现异常，需要快速回退到上一版 admin 镜像。

## 1. 确认当前镜像与容器

```powershell
docker ps --filter "name=linkhub-admin"
docker images | findstr shortlink-main-admin
```

## 2. 回滚到上一版镜像

如果上一版镜像已经存在（例如 `<previous-tag>`）：

```powershell
docker stop linkhub-admin
docker rm linkhub-admin
docker run -d --name linkhub-admin --network shortlink-main_linkhub-network -p 8002:8002 <previous-tag>
```

如果使用 compose 管理并且上一版是本地已保存镜像，可先给旧镜像打 tag 后再启动：

```powershell
docker tag <image-id> shortlink-main-admin:rollback
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d admin
```

## 3. 验证回滚结果

```powershell
curl http://localhost:8002/actuator/health
```

验证登录接口（正常请求不报系统异常）：

```powershell
powershell -Command "Invoke-RestMethod -Uri 'http://localhost:9090/api/short-link/admin/v1/user/login' -Method Post -ContentType 'application/json' -Body (@{username='admin';password='admin123456'} | ConvertTo-Json -Compress) | ConvertTo-Json -Compress"
```

## 4. 回滚后观察项

- 观察 `linkhub-admin` 日志是否恢复稳定。
- 确认前端登录可用。
- 确认未出现大面积 `B000001`。
