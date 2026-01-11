#!/bin/bash
# ============================================================================
# API Test Script for Job Orchestration Platform
# ============================================================================

BASE_URL="http://localhost:8080/api"

echo "======================================"
echo "Job Orchestration Platform - API Tests"
echo "======================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# Check if jq is available for pretty printing
if command -v jq &> /dev/null; then
    PRETTY="jq ."
else
    PRETTY="cat"
fi

# Test 1: Health Check
echo -e "${CYAN}1. Health Check${NC}"
echo "   GET /actuator/health"
curl -s http://localhost:8080/actuator/health | $PRETTY
echo ""

# Test 2: Create a PROCESS_DATA job
echo -e "${CYAN}2. Create PROCESS_DATA Job${NC}"
echo "   POST /api/jobs"
JOB1=$(curl -s -X POST "$BASE_URL/jobs" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Data Processing Test",
    "type": "PROCESS_DATA",
    "priority": 7,
    "payload": {"input": "test data", "format": "json"}
  }')
echo "$JOB1" | $PRETTY
JOB1_ID=$(echo "$JOB1" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo -e "   ${GREEN}Created job ID: $JOB1_ID${NC}"
echo ""

# Test 3: Create a SEND_EMAIL job
echo -e "${CYAN}3. Create SEND_EMAIL Job${NC}"
echo "   POST /api/jobs"
JOB2=$(curl -s -X POST "$BASE_URL/jobs" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Welcome Email",
    "type": "SEND_EMAIL",
    "priority": 9,
    "payload": {"to": "user@example.com", "template": "welcome"}
  }')
echo "$JOB2" | $PRETTY
JOB2_ID=$(echo "$JOB2" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo -e "   ${GREEN}Created job ID: $JOB2_ID${NC}"
echo ""

# Test 4: Create a GENERATE_REPORT job
echo -e "${CYAN}4. Create GENERATE_REPORT Job${NC}"
echo "   POST /api/jobs"
curl -s -X POST "$BASE_URL/jobs" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Monthly Report",
    "type": "GENERATE_REPORT",
    "priority": 5,
    "payload": {"month": "January", "year": 2026}
  }' | $PRETTY
echo ""

# Test 5: Create a SYNC_DATA job
echo -e "${CYAN}5. Create SYNC_DATA Job${NC}"
echo "   POST /api/jobs"
curl -s -X POST "$BASE_URL/jobs" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "User Sync",
    "type": "SYNC_DATA",
    "priority": 6,
    "payload": {"source": "ldap", "target": "database"}
  }' | $PRETTY
echo ""

# Wait for processing
echo -e "${YELLOW}Waiting 5 seconds for jobs to process...${NC}"
sleep 5

# Test 6: Get all jobs
echo -e "${CYAN}6. Get All Jobs${NC}"
echo "   GET /api/jobs"
curl -s "$BASE_URL/jobs" | $PRETTY
echo ""

# Test 7: Get job statistics
echo -e "${CYAN}7. Get Job Statistics${NC}"
echo "   GET /api/jobs/stats"
curl -s "$BASE_URL/jobs/stats" | $PRETTY
echo ""

# Test 8: Get specific job (if we have an ID)
if [ -n "$JOB1_ID" ]; then
    echo -e "${CYAN}8. Get Specific Job${NC}"
    echo "   GET /api/jobs/$JOB1_ID"
    curl -s "$BASE_URL/jobs/$JOB1_ID" | $PRETTY
    echo ""
fi

# Test 9: Filter by status
echo -e "${CYAN}9. Get Jobs by Status (PENDING)${NC}"
echo "   GET /api/jobs?status=PENDING"
curl -s "$BASE_URL/jobs?status=PENDING" | $PRETTY
echo ""

# Test 10: Filter by status
echo -e "${CYAN}10. Get Jobs by Status (COMPLETED)${NC}"
echo "   GET /api/jobs?status=COMPLETED"
curl -s "$BASE_URL/jobs?status=COMPLETED" | $PRETTY
echo ""

echo "======================================"
echo -e "${GREEN}API Tests Complete!${NC}"
echo "======================================"
