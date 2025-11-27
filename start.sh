#!/bin/bash

# Auth Platform Start Script
# This script starts Redis, OAuth2 Server, and Resource Server

set -e

AUTH_PLATFORM_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$AUTH_PLATFORM_DIR"

echo "🚀 Starting Auth Platform services..."

# Check if required files exist
if [ ! -f "docker-compose.yml" ]; then
    echo "❌ Error: docker-compose.yml not found in $AUTH_PLATFORM_DIR"
    exit 1
fi

if [ ! -f "gradlew" ]; then
    echo "❌ Error: gradlew not found in $AUTH_PLATFORM_DIR"
    exit 1
fi

# Start Redis
echo "📦 Starting Redis container..."
docker-compose up -d redis

# Wait for Redis to be ready
echo "⏳ Waiting for Redis to be ready..."
COUNTER=0
until docker-compose exec -T redis redis-cli ping 2>/dev/null | grep -q PONG; do
    sleep 1
    COUNTER=$((COUNTER + 1))
    if [ $COUNTER -gt 30 ]; then
        echo "❌ Redis failed to start within 30 seconds"
        exit 1
    fi
done
echo "✅ Redis is ready"

# Start OAuth2 Server in background
echo "🔐 Starting OAuth2 Server (port 8081)..."
./gradlew :oauth2-server:bootRun > oauth2-server.log 2>&1 &
OAUTH2_PID=$!
echo $OAUTH2_PID > oauth2-server.pid

# Start Resource Server in background
echo "🛡️  Starting Resource Server (port 8082)..."
./gradlew :resource-server:bootRun > resource-server.log 2>&1 &
RESOURCE_PID=$!
echo $RESOURCE_PID > resource-server.pid

echo "📝 Process IDs saved to oauth2-server.pid and resource-server.pid"
echo "📊 Logs are written to oauth2-server.log and resource-server.log"

# Wait a bit and check if services are starting
echo "⏳ Waiting for services to start..."
sleep 5

if ps -p $OAUTH2_PID > /dev/null; then
    echo "✅ OAuth2 Server is running (PID: $OAUTH2_PID)"
else
    echo "❌ OAuth2 Server failed to start"
fi

if ps -p $RESOURCE_PID > /dev/null; then
    echo "✅ Resource Server is running (PID: $RESOURCE_PID)"
else
    echo "❌ Resource Server failed to start"
fi

echo ""
echo "🎉 Auth Platform started successfully!"
echo "📍 Services:"
echo "   - Redis:           localhost:6379"
echo "   - OAuth2 Server:   http://localhost:8081"
echo "   - Resource Server: http://localhost:8082"
echo ""
echo "🔍 To check logs:"
echo "   tail -f oauth2-server.log"
echo "   tail -f resource-server.log"
echo ""
echo "🛑 To stop services, run: ./stop.sh"