@echo off
chcp 65001 >nul
echo ========================================
echo   ShortLink 项目一键部署脚本
echo ========================================
echo.

echo [1/4] 清理旧的容器和数据...
docker compose down -v
echo.

echo [2/4] Maven 打包项目...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo ❌ Maven 打包失败！
    pause
    exit /b 1
)
echo ✅ Maven 打包成功
echo.

echo [3/4] 构建 Docker 镜像...
docker compose build
if errorlevel 1 (
    echo ❌ Docker 构建失败！
    pause
    exit /b 1
)
echo ✅ Docker 构建成功
echo.

echo [4/4] 启动所有服务...
docker compose up -d
if errorlevel 1 (
    echo ❌ 服务启动失败！
    pause
    exit /b 1
)
echo ✅ 服务启动成功
echo.

echo ========================================
echo   部署完成！
echo ========================================
echo.
echo 访问地址：
echo   前端页面: http://localhost
echo   Project API: http://localhost/api/short-link/v1
echo.
echo 查看日志: docker compose logs -f
echo 停止服务: docker compose down
echo.
pause
