#!/bin/bash

# =============================================
# LinkHub 快速启动脚本 (Linux/Mac)
# =============================================

clear
echo ""
echo "╔═══════════════════════════════════════════╗"
echo "║           LinkHub - 短链接平台             ║"
echo "╚═══════════════════════════════════════════╝"
echo ""
echo "请选择启动模式："
echo ""
echo "  [1] Dev 模式  - 性能优先，不限制内存"
echo "  [2] Prod 模式 - 4GB 服务器优化"
echo "  [3] 查看状态"
echo "  [4] 停止服务"
echo ""
read -p "请输入选择 [1-4]: " choice

case $choice in
1)
    echo ""
    echo "🚀 正在启动 Dev 模式..."
    docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
    echo ""
    echo "✅ Dev 模式启动成功！"
    show_access
    ;;
2)
    echo ""
    echo "🚀 正在启动 Prod 模式 (4GB 优化)..."
    docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
    echo ""
    echo "✅ Prod 模式启动成功！"
    show_access
    ;;
3)
    echo ""
    echo "📊 正在查看服务状态..."
    docker-compose ps
    ;;
4)
    echo ""
    echo "⚠️ 正在停止所有服务..."
    docker-compose down
    ;;
*)
    echo "❌ 无效的选择！"
    ;;
esac

show_access() {
    echo ""
    echo "╔═══════════════════════════════════════════════════╗"
    echo "║                    服务访问地址                    ║"
    echo "╠═══════════════════════════════════════════════════╣"
    echo "║ 短链接控制台     - http://localhost:9090           ║"
    echo "║ 网关监控 API      - http://localhost:8000/api/v1/metrics ║"
    echo "║ 日志查询系统     - http://localhost:3001           ║"
    echo "║ Nacos 控制台    - http://localhost:8848/nacos      ║"
    echo "╚═══════════════════════════════════════════════════╝"
    echo ""
    echo "📝 提示：访问 http://localhost:9090 即可开始使用！"
    echo ""
}
