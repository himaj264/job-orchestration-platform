# Event-Driven Distributed Job Orchestration Platform

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green.svg)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.6-blue.svg)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-7.2-red.svg)](https://redis.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://docs.docker.com/compose/)

A distributed job orchestration system built with Spring Boot microservices and Apache Kafka for asynchronous job processing. This platform demonstrates enterprise-grade patterns for building scalable, fault-tolerant distributed systems.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Configuration](#configuration)
- [Scaling Workers](#scaling-workers)
- [Monitoring & Observability](#monitoring--observability)
- [Design Decisions](#design-decisions)
- [Troubleshooting](#troubleshooting)

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

## Project Structure

```
job-orchestration-platform/
├── README.md                          # This file
├── docker-compose.yml                 # Infrastructure setup
├── .gitignore                         # Git ignore rules
│
├── orchestrator-service/              # Job orchestration microservice
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/jobplatform/orchestrator/
│       │   ├── OrchestratorApplication.java
│       │   ├── controller/
│       │   │   └── JobController.java
│       │   ├── service/
│       │   │   ├── JobService.java
│       │   │   └── KafkaProducerService.java
│       │   ├── consumer/
│       │   │   └── JobStatusConsumer.java
│       │   ├── model/
│       │   │   ├── Job.java
│       │   │   └── JobStatus.java
│       │   ├── repository/
│       │   │   └── JobRepository.java
│       │   ├── dto/
│       │   │   ├── JobRequest.java
│       │   │   ├── JobResponse.java
│       │   │   └── JobEvent.java
│       │   └── config/
│       │       └── KafkaConfig.java
│       └── resources/
│           └── application.yml
│
├── worker-service/                    # Job execution microservice
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/jobplatform/worker/
│       │   ├── WorkerApplication.java
│       │   ├── consumer/
│       │   │   └── JobConsumer.java
│       │   ├── service/
│       │   │   ├── JobExecutorService.java
│       │   │   └── IdempotencyService.java
│       │   ├── dto/
│       │   │   └── JobEvent.java
│       │   └── config/
│       │       ├── KafkaConsumerConfig.java
│       │       └── RedisConfig.java
│       └── resources/
│           └── application.yml
│
└── docs/
    └── architecture.md                # Detailed architecture documentation
```

## Prerequisites

### Hardware Requirements (Optimized for M2 Mac 8GB RAM)
- **Minimum RAM**: 8GB (configuration optimized for this)
- **Free Disk Space**: 5GB for Docker images
- **CPU**: Apple M2 or equivalent

### Software Requirements
- **Docker Desktop**: v4.0+ with Docker Compose v2
- **Java 17**: For local development (optional if using Docker)
- **Maven 3.8+**: For building services (optional if using Docker)
- **Git**: For version control

### Installation Links
- [Docker Desktop for Mac](https://docs.docker.com/desktop/install/mac-install/)
- [SDKMAN for Java](https://sdkman.io/) (recommended for Mac)

## Quick Start

### Option 1: Docker Compose (Recommended)

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

### Memory-Optimized Docker Settings (8GB RAM Mac)

Update your Docker Desktop settings:
- **Memory**: 4GB
- **Swap**: 1GB
- **CPUs**: 4

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
                  │
                  │ (max retries exceeded)
                  ▼
            DEAD_LETTER
```

## Configuration

### Environment Variables

#### Orchestrator Service
| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/jobdb` | PostgreSQL connection |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `SERVER_PORT` | `8080` | HTTP port |

#### Worker Service
| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `SPRING_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_REDIS_PORT` | `6379` | Redis port |
| `WORKER_CONCURRENCY` | `3` | Concurrent job processors |
| `WORKER_RETRY_MAX_ATTEMPTS` | `3` | Max retry attempts |

### Kafka Topics Configuration

| Topic | Partitions | Replication | Purpose |
|-------|------------|-------------|---------|
| `job-requests` | 3 | 1 | Job execution requests |
| `job-status` | 3 | 1 | Status update events |
| `job-dlq` | 1 | 1 | Dead letter queue |

## Scaling Workers

### Horizontal Scaling with Docker Compose

```bash
# Scale to 3 worker instances
docker-compose up -d --scale worker=3

# Verify workers are running
docker-compose ps
```

### Consumer Group Behavior

Workers automatically join the `job-worker-group` consumer group:
- Kafka distributes partitions among active workers
- If a worker fails, its partitions are reassigned
- Adding workers triggers rebalancing

### Scaling Considerations

| Workers | Throughput | Memory Usage |
|---------|------------|--------------|
| 1 | ~100 jobs/min | ~512MB |
| 2 | ~180 jobs/min | ~1GB |
| 3 | ~250 jobs/min | ~1.5GB |

*Note: Actual throughput depends on job complexity*

## Monitoring & Observability

### Health Checks

```bash
# Orchestrator health
curl http://localhost:8080/actuator/health

# Worker health
curl http://localhost:8081/actuator/health
```

### Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f orchestrator
docker-compose logs -f worker

# Filter by job ID
docker-compose logs | grep "job-id=550e8400"
```

### Kafka Management

```bash
# List topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# View consumer groups
docker-compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Check consumer lag
docker-compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group job-worker-group

# View messages in DLQ
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic job-dlq \
  --from-beginning
```

### Redis Monitoring

```bash
# Connect to Redis CLI
docker-compose exec redis redis-cli

# Check idempotency keys
KEYS job:idempotency:*

# Monitor real-time commands
MONITOR
```

### PostgreSQL Queries

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U jobuser -d jobdb

# View recent jobs
SELECT id, name, status, created_at FROM jobs ORDER BY created_at DESC LIMIT 10;

# Job statistics
SELECT status, COUNT(*) FROM jobs GROUP BY status;

# Failed jobs analysis
SELECT * FROM jobs WHERE status = 'FAILED' ORDER BY updated_at DESC;
```

## Design Decisions

### Why Kafka over RabbitMQ?
1. **Log-based storage**: Messages persist after consumption, enabling replay
2. **Higher throughput**: Better suited for high-volume job processing
3. **Consumer groups**: Native support for horizontal scaling
4. **Partitioning**: Enables parallel processing with ordering guarantees

### Why Redis for Idempotency?
1. **Speed**: Sub-millisecond lookups for duplicate detection
2. **TTL support**: Automatic cleanup of old idempotency keys
3. **Atomic operations**: SETNX ensures race-condition-free checks

### Why PostgreSQL?
1. **ACID compliance**: Reliable job metadata storage
2. **Rich querying**: Complex job filtering and analytics
3. **JSON support**: Flexible payload storage with JSONB

### Retry Strategy
- **Exponential backoff**: Prevents thundering herd on failures
- **Max attempts**: 3 (configurable) before moving to DLQ
- **Backoff multiplier**: 2x (1s, 2s, 4s delays)

### Idempotency Implementation
```
Job arrives → Check Redis for job ID
  ├── Key exists → Skip (already processed)
  └── Key missing → Set key with TTL → Process job
```

## Troubleshooting

### Common Issues

#### 1. Kafka Connection Refused
```
Error: Connection to node -1 could not be established
```
**Solution**: Wait for Kafka to fully start (~30 seconds after `docker-compose up`)

#### 2. Out of Memory (Docker)
```
Error: Container killed due to OOM
```
**Solution**: Reduce Kafka heap size in `docker-compose.yml`:
```yaml
KAFKA_HEAP_OPTS: "-Xmx256m -Xms256m"
```

#### 3. Port Already in Use
```
Error: Bind for 0.0.0.0:8080 failed: port is already allocated
```
**Solution**: Stop conflicting service or change port in `docker-compose.yml`

#### 4. Consumer Lag Building Up
```bash
# Check lag
docker-compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group job-worker-group
```
**Solution**: Scale workers: `docker-compose up -d --scale worker=3`

#### 5. Jobs Stuck in PENDING
**Possible causes**:
- Worker service not running
- Kafka topic not created
- Consumer group issues

**Debug steps**:
```bash
# Check worker logs
docker-compose logs worker

# Verify topic exists
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Check consumer group
docker-compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group job-worker-group
```

### Reset Everything

```bash
# Stop and remove all containers, volumes, and networks
docker-compose down -v

# Remove all images (optional)
docker-compose down -v --rmi all

# Fresh start
docker-compose up -d
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Redis Documentation](https://redis.io/documentation)

---

**Author**: Hima Kammachi
**Course**: Master's Program - Distributed Systems
**Year**: 2026
