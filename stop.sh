#!/bin/bash

# Auth Platform Stop Script
# This script stops Redis, OAuth2 Server, and Resource Server

AUTH_PLATFORM_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$AUTH_PLATFORM_DIR"

echo "🛑 Stopping Auth Platform services..."

# Stop OAuth2 Server
if [ -f "oauth2-server.pid" ]; then
    OAUTH2_PID=$(cat oauth2-server.pid)
    if ps -p $OAUTH2_PID > /dev/null; then
        echo "🔐 Stopping OAuth2 Server (PID: $OAUTH2_PID)..."
        kill $OAUTH2_PID
        echo "✅ OAuth2 Server stopped"
    else
        echo "⚠️  OAuth2 Server is not running"
    fi
    rm -f oauth2-server.pid
else
    echo "⚠️  oauth2-server.pid not found"
fi

# Stop Resource Server
if [ -f "resource-server.pid" ]; then
    RESOURCE_PID=$(cat resource-server.pid)
    if ps -p $RESOURCE_PID > /dev/null; then
        echo "🛡️  Stopping Resource Server (PID: $RESOURCE_PID)..."
        kill $RESOURCE_PID
        echo "✅ Resource Server stopped"
    else
        echo "⚠️  Resource Server is not running"
    fi
    rm -f resource-server.pid
else
    echo "⚠️  resource-server.pid not found"
fi

# Stop Redis
echo "📦 Stopping Redis container..."
docker-compose down

echo ""
echo "🎉 Auth Platform stopped successfully!"
echo ""
echo "🧹 Clean up:"
echo "   - Log files (oauth2-server.log, resource-server.log) are preserved"
echo "   - To remove logs: rm -f *.log"