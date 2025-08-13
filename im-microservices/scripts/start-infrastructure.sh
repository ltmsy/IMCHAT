#!/bin/bash

# IM系统基础设施服务启动脚本
# 作者: IM开发团队
# 日期: 2024-01-XX

set -e

echo "🚀 启动IM系统基础设施服务..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行，请先启动Docker"
    exit 1
fi

# 检查Docker Compose是否可用
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose未安装，请先安装Docker Compose"
    exit 1
fi

# 切换到docker目录
cd "$(dirname "$0")/../docker"

echo "📁 切换到目录: $(pwd)"

# 停止现有服务（如果存在）
echo "🛑 停止现有服务..."
docker-compose down

# 启动所有服务
echo "🚀 启动所有服务..."
docker-compose up -d

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
echo "🔍 检查服务状态..."
docker-compose ps

# 检查MySQL连接
echo "🔍 检查MySQL连接..."
if docker exec im-mysql-master mysql -uroot -p123456 -e "SELECT 1" > /dev/null 2>&1; then
    echo "✅ MySQL主库连接正常"
else
    echo "❌ MySQL主库连接失败"
fi

# 检查Redis连接
echo "🔍 检查Redis连接..."
if docker exec im-redis redis-cli ping > /dev/null 2>&1; then
    echo "✅ Redis连接正常"
else
    echo "❌ Redis连接失败"
fi

# 检查NATS连接
echo "🔍 检查NATS连接..."
if docker exec im-nats nats-server --version > /dev/null 2>&1; then
    echo "✅ NATS服务运行正常"
else
    echo "❌ NATS服务异常"
fi

# 检查MinIO连接
echo "🔍 检查MinIO连接..."
if curl -s http://localhost:9000/minio/health/live > /dev/null 2>&1; then
    echo "✅ MinIO连接正常"
else
    echo "❌ MinIO连接失败"
fi

echo ""
echo "🎉 基础设施服务启动完成！"
echo ""
echo "📊 服务访问地址："
echo "   MySQL主库: localhost:3306"
echo "   MySQL从库: localhost:3307"
echo "   Redis: localhost:6379"
echo "   NATS: localhost:4222"
echo "   NATS管理: localhost:8222"
echo "   MinIO: localhost:9000"
echo "   MinIO控制台: localhost:9001"
echo "   Redis管理: localhost:8081"
echo "   MySQL管理: localhost:8082"
echo ""
echo "🔧 管理命令："
echo "   查看服务状态: docker-compose ps"
echo "   查看服务日志: docker-compose logs [服务名]"
echo "   停止所有服务: docker-compose down"
echo "   重启服务: docker-compose restart [服务名]"
echo ""
echo "💡 提示：首次启动可能需要几分钟时间，请耐心等待..." 