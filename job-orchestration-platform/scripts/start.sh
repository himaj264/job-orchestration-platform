#!/bin/bash
# ============================================================================
# Start Script for Job Orchestration Platform
# ============================================================================

set -e

echo "======================================"
echo "Job Orchestration Platform - Startup"
echo "======================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running. Please start Docker Desktop first.${NC}"
    exit 1
fi

echo -e "${GREEN}Docker is running.${NC}"

# Check available memory
echo "Checking system resources..."

# Start services
echo -e "${YELLOW}Starting all services...${NC}"
docker-compose up -d

# Wait for services to be healthy
echo "Waiting for services to be ready..."
sleep 10

# Check service status
echo -e "${GREEN}Checking service status...${NC}"
docker-compose ps

# Wait for Kafka to be ready
echo "Waiting for Kafka to be fully ready (this may take 30-60 seconds)..."
until docker-compose exec -T kafka kafka-topics --list --bootstrap-server localhost:9092 > /dev/null 2>&1; do
    echo "  Kafka not ready yet, waiting..."
    sleep 5
done
echo -e "${GREEN}Kafka is ready!${NC}"

# List Kafka topics
echo "Kafka topics:"
docker-compose exec -T kafka kafka-topics --list --bootstrap-server localhost:9092

echo ""
echo -e "${GREEN}======================================"
echo "All services are up and running!"
echo "======================================${NC}"
echo ""
echo "Available endpoints:"
echo "  - REST API:        http://localhost:8080/api/jobs"
echo "  - Health Check:    http://localhost:8080/actuator/health"
echo "  - Worker Health:   http://localhost:8081/actuator/health"
echo ""
echo "Quick test command:"
echo "  curl -X POST http://localhost:8080/api/jobs \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"name\": \"test-job\", \"type\": \"PROCESS_DATA\", \"payload\": {\"test\": true}}'"
echo ""
echo "View logs:    docker-compose logs -f"
echo "Stop:         docker-compose down"
echo ""
