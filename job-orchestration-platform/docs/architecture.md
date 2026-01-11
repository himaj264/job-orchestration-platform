# Architecture Documentation

## Event-Driven Distributed Job Orchestration Platform

This document provides a detailed technical overview of the system architecture, design patterns, and implementation decisions.

## Table of Contents

1. [System Overview](#system-overview)
2. [Component Architecture](#component-architecture)
3. [Data Flow](#data-flow)
4. [Message Patterns](#message-patterns)
5. [Reliability Patterns](#reliability-patterns)
6. [Scalability](#scalability)
7. [Data Model](#data-model)
8. [Security Considerations](#security-considerations)
9. [Performance Considerations](#performance-considerations)

---

## System Overview

### High-Level Architecture

The platform follows an event-driven microservices architecture with three main layers:

```
┌──────────────────────────────────────────────────────────────────┐
│                         API LAYER                                 │
│                    (REST Controllers)                             │
└────────────────────────────┬─────────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────────┐
│                      SERVICE LAYER                                │
│           (Business Logic & Orchestration)                        │
└────────────────────────────┬─────────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────────┐
│                    MESSAGING LAYER                                │
│                  (Apache Kafka Topics)                            │
└────────────────────────────┬─────────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────────┐
│                     DATA LAYER                                    │
│              (PostgreSQL + Redis)                                 │
└──────────────────────────────────────────────────────────────────┘
```

### Design Principles

1. **Loose Coupling**: Services communicate via Kafka messages, not direct calls
2. **Event Sourcing**: Job state changes are captured as events
3. **Idempotency**: Duplicate message handling ensures correctness
4. **Fault Tolerance**: Retry logic and DLQ handle failures gracefully
5. **Horizontal Scalability**: Stateless workers scale independently

---

## Component Architecture

### Orchestrator Service

**Purpose**: Central coordination point for job management

**Responsibilities**:
- Accept job submissions via REST API
- Persist job metadata to PostgreSQL
- Publish job events to Kafka
- Consume status updates from workers
- Provide query and statistics endpoints

**Key Classes**:
```
OrchestratorApplication.java     - Spring Boot entry point
├── controller/
│   └── JobController.java       - REST API endpoints
├── service/
│   ├── JobService.java          - Business logic
│   └── KafkaProducerService.java - Event publishing
├── consumer/
│   └── JobStatusConsumer.java   - Status update handling
├── model/
│   ├── Job.java                 - JPA entity
│   └── JobStatus.java           - Status enumeration
├── repository/
│   └── JobRepository.java       - Data access
└── config/
    └── KafkaConfig.java         - Kafka configuration
```

### Worker Service

**Purpose**: Execute jobs from the queue

**Responsibilities**:
- Consume jobs from Kafka `job-requests` topic
- Check idempotency using Redis
- Execute jobs based on type
- Publish status updates to Kafka
- Handle failures and retries

**Key Classes**:
```
WorkerApplication.java           - Spring Boot entry point
├── consumer/
│   └── JobConsumer.java         - Kafka message consumer
├── service/
│   ├── JobExecutorService.java  - Job execution logic
│   ├── IdempotencyService.java  - Duplicate detection
│   └── KafkaStatusPublisher.java - Status publishing
└── config/
    ├── KafkaConsumerConfig.java - Consumer configuration
    ├── KafkaProducerConfig.java - Producer configuration
    └── RedisConfig.java         - Redis configuration
```

---

## Data Flow

### Job Submission Flow

```
1. Client sends POST /api/jobs
         │
         ▼
2. JobController validates request
         │
         ▼
3. JobService creates Job entity
         │
         ▼
4. Job saved to PostgreSQL (status: PENDING)
         │
         ▼
5. KafkaProducerService publishes to "job-requests" topic
         │
         ▼
6. Response returned to client (201 Created)
```

### Job Execution Flow

```
1. Worker's JobConsumer receives message from Kafka
         │
         ▼
2. IdempotencyService checks Redis for duplicate
         │
         ├── Duplicate found → Skip, acknowledge message
         │
         ▼
3. Set idempotency key in Redis
         │
         ▼
4. Publish "JOB_STARTED" status to "job-status" topic
         │
         ▼
5. JobExecutorService executes job based on type
         │
         ├── Success → Publish "JOB_COMPLETED"
         │
         └── Failure → Check retry count
                 │
                 ├── Retries remaining → Publish "JOB_FAILED" (will retry)
                 │
                 └── Max retries → Publish to "job-dlq"
         │
         ▼
6. Acknowledge Kafka message
```

### Status Update Flow

```
1. Worker publishes status to "job-status" topic
         │
         ▼
2. Orchestrator's JobStatusConsumer receives message
         │
         ▼
3. JobService.updateJobStatus() updates database
         │
         ├── RUNNING → Update startedAt, workerId
         │
         ├── COMPLETED → Update result, completedAt
         │
         ├── FAILED → Update errorMessage, check retries
         │
         └── DEAD_LETTER → Mark as terminal state
```

---

## Message Patterns

### Kafka Topics

| Topic | Purpose | Key | Value | Retention |
|-------|---------|-----|-------|-----------|
| `job-requests` | Job execution queue | jobId (UUID) | JobEvent | 7 days |
| `job-status` | Status updates | jobId (UUID) | JobEvent | 7 days |
| `job-dlq` | Dead letter queue | jobId (UUID) | JobEvent | 30 days |

### Message Schema (JobEvent)

```json
{
  "jobId": "uuid",
  "name": "string",
  "type": "PROCESS_DATA | SEND_EMAIL | GENERATE_REPORT | SYNC_DATA",
  "status": "PENDING | RUNNING | COMPLETED | FAILED | DEAD_LETTER",
  "priority": "integer (1-10)",
  "payload": "object",
  "result": "object",
  "errorMessage": "string",
  "retryCount": "integer",
  "maxRetries": "integer",
  "workerId": "string",
  "timestamp": "ISO-8601 datetime",
  "eventType": "JOB_CREATED | JOB_STARTED | JOB_COMPLETED | JOB_FAILED | JOB_DEAD_LETTER"
}
```

### Consumer Groups

| Group ID | Service | Topics | Behavior |
|----------|---------|--------|----------|
| `job-worker-group` | Worker | `job-requests` | Competing consumers, partitioned |
| `orchestrator-status-group` | Orchestrator | `job-status` | Single logical consumer |

---

## Reliability Patterns

### At-Least-Once Delivery

**Implementation**:
- Manual Kafka acknowledgment (AckMode.MANUAL)
- Idempotency keys in Redis prevent duplicate processing
- Messages only acknowledged after successful processing

**Code Path**:
```java
// JobConsumer.java
@KafkaListener(...)
public void consumeJob(ConsumerRecord<String, JobEvent> record, Acknowledgment ack) {
    try {
        if (!idempotencyService.tryAcquire(event.getJobId())) {
            ack.acknowledge();  // Duplicate, skip
            return;
        }
        // ... process job ...
        ack.acknowledge();  // Success
    } catch (Exception e) {
        // Don't acknowledge - will be redelivered
        // Or handle and acknowledge to prevent infinite loop
    }
}
```

### Idempotency Pattern

**Purpose**: Ensure each job is processed exactly once

**Implementation**:
```
┌─────────────────────────────────────────────────────────────────┐
│                         REDIS                                    │
│                                                                  │
│  Key: job:idempotency:{jobId}                                   │
│  Value: "processing" | "completed" | "failed"                    │
│  TTL: 24 hours                                                   │
│                                                                  │
│  Operation: SETNX (SET if Not eXists)                           │
│  - Atomic check-and-set                                          │
│  - Returns true if key was created (first time)                  │
│  - Returns false if key exists (duplicate)                       │
└─────────────────────────────────────────────────────────────────┘
```

### Retry Pattern with Exponential Backoff

**Configuration**:
- Max retries: 3 (configurable)
- Initial backoff: 1 second
- Backoff multiplier: 2x
- Backoff sequence: 1s → 2s → 4s

**Flow**:
```
Attempt 1: Execute job
    └── FAILED → Wait 1s
Attempt 2: Execute job
    └── FAILED → Wait 2s
Attempt 3: Execute job
    └── FAILED → Wait 4s
Attempt 4: Would exceed max retries
    └── Move to DLQ
```

### Dead Letter Queue (DLQ)

**Purpose**: Isolate failed jobs for manual review

**When Jobs Move to DLQ**:
1. Max retry attempts exceeded
2. Unrecoverable errors (data corruption, invalid format)
3. Poison messages (cannot be deserialized)

**DLQ Processing**:
- Jobs in DLQ require manual intervention
- Admin can review, fix, and resubmit jobs
- 30-day retention for post-mortem analysis

---

## Scalability

### Horizontal Scaling

**Workers**:
```bash
# Scale to N workers
docker-compose up -d --scale worker=N
```

**Kafka Partitioning**:
- `job-requests` topic has 3 partitions
- Each partition assigned to one consumer in a group
- Maximum parallelism = number of partitions

**Scaling Matrix**:
| Workers | Active Partitions per Worker | Throughput |
|---------|------------------------------|------------|
| 1 | 3 | 1x |
| 2 | 1-2 | ~1.8x |
| 3 | 1 | ~2.5x |
| 4+ | 0-1 (some idle) | ~2.5x |

### Vertical Scaling

**JVM Tuning**:
```
-Xmx384m -Xms256m        # Heap size
-XX:+UseG1GC              # G1 garbage collector
-XX:MaxGCPauseMillis=100  # GC pause target
```

### Load Distribution

```
┌─────────────────────────────────────────────────────────────────┐
│                    KAFKA PARTITIONING                            │
│                                                                  │
│  job-requests topic                                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                         │
│  │Partition 0│ │Partition 1│ │Partition 2│                       │
│  └─────┬────┘ └─────┬────┘ └─────┬────┘                         │
│        │            │            │                               │
│        ▼            ▼            ▼                               │
│   ┌────────┐   ┌────────┐   ┌────────┐                          │
│   │Worker 1│   │Worker 2│   │Worker 3│                          │
│   └────────┘   └────────┘   └────────┘                          │
│                                                                  │
│  Key-based partitioning ensures all events for                   │
│  same job go to same partition (ordering)                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Model

### PostgreSQL Schema

```sql
-- Main jobs table
CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,      -- PROCESS_DATA, SEND_EMAIL, etc.
    status VARCHAR(20) NOT NULL,     -- PENDING, RUNNING, COMPLETED, etc.
    priority INTEGER DEFAULT 5,
    payload JSONB,                   -- Job input data
    result JSONB,                    -- Job output data
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    worker_id VARCHAR(100)
);

-- Execution history for audit
CREATE TABLE job_execution_history (
    id UUID PRIMARY KEY,
    job_id UUID REFERENCES jobs(id),
    status VARCHAR(20) NOT NULL,
    message TEXT,
    worker_id VARCHAR(100),
    execution_time_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE
);
```

### Redis Data Structures

```
# Idempotency Keys
Key:    job:idempotency:{jobId}
Value:  "processing" | "completed" | "failed"
TTL:    86400 seconds (24 hours)
```

---

## Security Considerations

### Network Security

```yaml
# Docker network isolation
networks:
  job-platform-network:
    driver: bridge
    internal: false  # Set true in production
```

### Authentication/Authorization

**Current State**: None (development setup)

**Production Recommendations**:
1. Enable Kafka SASL/SSL authentication
2. Add Spring Security to REST API
3. Use Redis AUTH
4. PostgreSQL SSL connections

### Data Protection

**Sensitive Data Handling**:
- Payloads may contain sensitive data
- Consider encryption at rest for PostgreSQL
- Redis data should be encrypted in transit

---

## Performance Considerations

### Memory Optimization (8GB RAM)

**Component Allocation**:
| Component | Memory | Notes |
|-----------|--------|-------|
| Kafka | 512MB | Reduced heap |
| Zookeeper | 256MB | Minimal |
| PostgreSQL | 384MB | Shared buffers |
| Redis | 128MB | maxmemory |
| Orchestrator | 384MB | -Xmx384m |
| Worker (each) | 256MB | -Xmx256m |
| **Total** | ~2GB | Fits in 4GB Docker allocation |

### Throughput Optimization

**Kafka Producer**:
```properties
batch.size=16384          # Batch messages
linger.ms=5               # Wait for batch
acks=all                  # Wait for replication
```

**Kafka Consumer**:
```properties
max.poll.records=10       # Process in batches
fetch.min.bytes=1         # Don't wait
fetch.max.wait.ms=500     # Quick polling
```

### Monitoring Metrics

**Key Metrics to Track**:
1. Job throughput (jobs/minute)
2. Consumer lag (messages behind)
3. Execution time distribution
4. Error rate by job type
5. DLQ accumulation rate

---

## Appendix

### Environment Variables Reference

| Variable | Service | Default | Description |
|----------|---------|---------|-------------|
| `SPRING_DATASOURCE_URL` | Orchestrator | localhost | PostgreSQL URL |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Both | localhost:9092 | Kafka brokers |
| `SPRING_DATA_REDIS_HOST` | Worker | localhost | Redis host |
| `WORKER_CONCURRENCY` | Worker | 3 | Consumer threads |
| `WORKER_RETRY_MAX_ATTEMPTS` | Worker | 3 | Max retries |

### Useful Commands

```bash
# View Kafka consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 \
    --describe --group job-worker-group

# View messages in topic
kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic job-requests --from-beginning

# Redis CLI
redis-cli KEYS "job:idempotency:*"

# PostgreSQL job stats
psql -U jobuser -d jobdb -c "SELECT status, COUNT(*) FROM jobs GROUP BY status;"
```

---

**Document Version**: 1.0
**Author**: Hima Kammachi
**Last Updated**: 2026
