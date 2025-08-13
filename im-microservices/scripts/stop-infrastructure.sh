#!/bin/bash

# IM系统基础设施服务停止脚本
# 作者: IM开发团队
# 日期: 2024-01-XX

set -e

echo "🛑 停止IM系统基础设施服务..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行"
    exit 1
fi

# 切换到docker目录
cd "$(dirname "$0")/../docker"

echo "📁 切换到目录: $(pwd)"

# 停止所有服务
echo "🛑 停止所有服务..."
docker-compose down

# 检查服务状态
echo "🔍 检查服务状态..."
docker-compose ps

echo ""
echo "✅ 所有基础设施服务已停止"
echo ""
echo "💡 提示："
echo "   如需重新启动服务，请运行: ./scripts/start-infrastructure.sh"
echo "   如需清理数据卷，请运行: docker-compose down -v" 