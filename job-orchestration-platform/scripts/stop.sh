#!/bin/bash
# ============================================================================
# Stop Script for Job Orchestration Platform
# ============================================================================

echo "======================================"
echo "Job Orchestration Platform - Shutdown"
echo "======================================"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Stopping all services...${NC}"
docker-compose down

echo -e "${GREEN}All services stopped.${NC}"
echo ""
echo "To remove volumes (reset data): docker-compose down -v"
echo "To remove images:               docker-compose down -v --rmi all"
