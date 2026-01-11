# Event-Driven Distributed Job Orchestration Platform


A distributed job orchestration system built with Spring Boot microservices and Apache Kafka for asynchronous job processing. This platform demonstrates enterprise-grade patterns for building scalable, fault-tolerant distributed systems.


## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CLIENT APPLICATION                                 │
│                          (REST API Consumer)                                 │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │ HTTP REST
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ORCHESTRATOR SERVICE                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   REST API  │  │ Job Service │  │Kafka Producer│  │  Status Consumer   │ │
│  │  Controller │──│   Logic     │──│   Service   │  │  (Status Updates)  │ │
│  └─────────────┘  └─────────────┘  └──────┬──────┘  └──────────┬──────────┘ │
└───────────────────────────────────────────┼────────────────────┼────────────┘
                                            │                    │
                    ┌───────────────────────┼────────────────────┼────────────┐
                    │              APACHE KAFKA                  │            │
                    │  ┌────────────────┐  ┌────────────────┐   │            │
                    │  │  job-requests  │  │  job-status    │◄──┘            │
                    │  │     Topic      │  │    Topic       │                │
                    │  └───────┬────────┘  └────────────────┘                │
                    │          │           ┌────────────────┐                │
                    │          │           │   job-dlq      │                │
                    │          │           │    Topic       │                │
                    │          │           └────────────────┘                │
                    └──────────┼───────────────────────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ WORKER SERVICE  │  │ WORKER SERVICE  │  │ WORKER SERVICE  │
│   (Instance 1)  │  │   (Instance 2)  │  │   (Instance N)  │
│                 │  │                 │  │                 │
│ ┌─────────────┐ │  │ ┌─────────────┐ │  │ ┌─────────────┐ │
│ │   Kafka     │ │  │ │   Kafka     │ │  │ │   Kafka     │ │
│ │  Consumer   │ │  │ │  Consumer   │ │  │ │  Consumer   │ │
│ └──────┬──────┘ │  │ └──────┬──────┘ │  │ └──────┬──────┘ │
│        │        │  │        │        │  │        │        │
│ ┌──────▼──────┐ │  │ ┌──────▼──────┐ │  │ ┌──────▼──────┐ │
│ │ Idempotency │ │  │ │ Idempotency │ │  │ │ Idempotency │ │
│ │   Check     │ │  │ │   Check     │ │  │ │   Check     │ │
│ └──────┬──────┘ │  │ └──────┬──────┘ │  │ └──────┬──────┘ │
│        │        │  │        │        │  │        │        │
│ ┌──────▼──────┐ │  │ ┌──────▼──────┐ │  │ ┌──────▼──────┐ │
│ │    Job      │ │  │ │    Job      │ │  │ │    Job      │ │
│ │  Executor   │ │  │ │  Executor   │ │  │ │  Executor   │ │
│ └─────────────┘ │  │ └─────────────┘ │  │ └─────────────┘ │
└─────────────────┘  └─────────────────┘  └─────────────────┘
          │                    │                    │
          └────────────────────┼────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐
│     REDIS       │  │   POSTGRESQL    │
│  (Idempotency   │  │  (Job Metadata  │
│    Cache)       │  │   & History)    │
└─────────────────┘  └─────────────────┘
```

## Key Features

### 1. Job Lifecycle Management
- Complete job state machine: `PENDING → RUNNING → COMPLETED/FAILED`
- Automatic status transitions with event publishing
- Job metadata persistence for audit trails

### 2. Asynchronous Processing with Kafka
- Decoupled job submission and execution
- Kafka topics for job requests, status updates, and dead letters
- Consumer groups enable horizontal scaling

### 3. Fault Tolerance & Reliability
- **Retry Logic**: Configurable retry attempts with exponential backoff
- **Dead-Letter Queue (DLQ)**: Failed jobs isolated for manual review
- **Idempotency**: Redis-based duplicate execution prevention

### 4. Horizontal Scalability
- Stateless worker services scale independently
- Kafka partitioning distributes load across workers
- Consumer group rebalancing handles worker failures

### 5. Observability
- Job execution history in PostgreSQL
- Structured logging for debugging
- Health check endpoints for monitoring

## Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| Backend Framework | Spring Boot 3.2 | Microservices foundation |
| Message Broker | Apache Kafka 3.6 | Asynchronous job distribution |
| Cache | Redis 7.2 | Idempotency and distributed locking |
| Database | PostgreSQL 15 | Job metadata and history |
| Containerization | Docker Compose | Local development environment |
| Build Tool | Maven | Dependency management |
| Java Version | Java 17 | LTS release |


### Software Requirements
- **Docker Desktop**: v4.0+ with Docker Compose v2
- **Java 17**: For local development (optional if using Docker)
- **Maven 3.8+**: For building services (optional if using Docker)
- **Git**: For version control



## Quick Start

### Option 1: Docker Compose 

This is the easiest way to run the entire platform locally.

```bash
# 1. Clone the repository
git clone https://github.com/yourusername/job-orchestration-platform.git
cd job-orchestration-platform

# 2. Start all services
docker-compose up -d

# 3. Wait for services to be healthy (about 30-60 seconds)
docker-compose ps

# 4. Test the API
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"name": "test-job", "payload": "Hello World", "type": "PROCESS_DATA"}'

# 5. Check job status
curl http://localhost:8080/api/jobs

# 6. Stop all services
docker-compose down
```

### Option 2: Local Development

For development and debugging:

```bash
# 1. Start infrastructure only
docker-compose up -d kafka zookeeper redis postgres

# 2. Wait for Kafka to be ready
docker-compose logs -f kafka  # Wait for "started" message, then Ctrl+C

# 3. Build services
cd orchestrator-service && mvn clean package -DskipTests && cd ..
cd worker-service && mvn clean package -DskipTests && cd ..

# 4. Run orchestrator (Terminal 1)
cd orchestrator-service
java -jar target/orchestrator-service-1.0.0.jar

# 5. Run worker (Terminal 2)
cd worker-service
java -jar target/worker-service-1.0.0.jar
```


## API Documentation

### Base URL
```
http://localhost:8080/api
```

### Endpoints

#### Create a Job
```http
POST /api/jobs
Content-Type: application/json

{
  "name": "data-processing-job",
  "payload": "{\"input\": \"process this data\"}",
  "type": "PROCESS_DATA",
  "priority": 5
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "data-processing-job",
  "status": "PENDING",
  "type": "PROCESS_DATA",
  "priority": 5,
  "createdAt": "2026-01-15T10:30:00Z",
  "updatedAt": "2026-01-15T10:30:00Z"
}
```

#### Get All Jobs
```http
GET /api/jobs?status=PENDING&page=0&size=20
```

#### Get Job by ID
```http
GET /api/jobs/{jobId}
```

#### Cancel a Job
```http
DELETE /api/jobs/{jobId}
```

#### Get Job Statistics
```http
GET /api/jobs/stats
```

**Response:**
```json
{
  "total": 150,
  "pending": 10,
  "running": 5,
  "completed": 130,
  "failed": 5
}
```

### Job Types
| Type | Description |
|------|-------------|
| `PROCESS_DATA` | Generic data processing task |
| `SEND_EMAIL` | Email notification job |
| `GENERATE_REPORT` | Report generation task |
| `SYNC_DATA` | Data synchronization job |

### Job Status Flow
```
PENDING ──────► RUNNING ──────► COMPLETED
                  │
                  │ (on failure)
                  ▼
               FAILED ──────► RETRY ──────► RUNNING
 

