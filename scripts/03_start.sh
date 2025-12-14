#!/bin/bash
set -e

echo "Step 3: Start Docker Environment"

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCKER_DIR="$BASE_DIR/tomcat_mysql_docker"

# Stop existing containers using ports 3306 or 8080 to avoid conflicts
echo "Checking for conflicting containers..."
CONFLICT_IDS=$(docker ps -q --filter "publish=3306" --filter "publish=8080")
if [ -n "$CONFLICT_IDS" ]; then
    echo "Stopping conflicting containers: $CONFLICT_IDS"
    docker stop $CONFLICT_IDS
    docker rm $CONFLICT_IDS
fi

# Run docker-compose
echo "Starting containers..."
cd "$DOCKER_DIR"
docker-compose up -d --build

echo "Waiting for MySQL to be healthy..."
# Simple wait loop
for i in {1..30}; do
    if docker exec demo_mysql mysqladmin ping -h localhost --silent; then
        echo "MySQL is up and running."
        break
    fi
    echo "Waiting for MySQL..."
    sleep 2
done

echo "Environment started. Ports: MySQL(3306), Tomcat(8080)"
