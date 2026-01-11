package com.jobplatform.orchestrator.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a job in the orchestration system.
 *
 * This entity stores all metadata about a job including its current status,
 * execution details, and results. It maps to the 'jobs' table in PostgreSQL.
 *
 * @author Hima Kammachi
 */
@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_status", columnList = "status"),
        @Index(name = "idx_jobs_type", columnList = "type"),
        @Index(name = "idx_jobs_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 5;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "jsonb")
    private Map<String, Object> result;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "worker_id", length = 100)
    private String workerId;

    /**
     * Set creation timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    /**
     * Update timestamp before updating.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Check if the job can be cancelled.
     *
     * @return true if the job is in a cancellable state
     */
    public boolean isCancellable() {
        return status == JobStatus.PENDING;
    }

    /**
     * Check if the job can be retried.
     *
     * @return true if the job can be retried
     */
    public boolean canRetry() {
        return status == JobStatus.FAILED && retryCount < maxRetries;
    }

    /**
     * Increment the retry count.
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Mark the job as running.
     *
     * @param workerId the ID of the worker processing this job
     */
    public void markAsRunning(String workerId) {
        this.status = JobStatus.RUNNING;
        this.workerId = workerId;
        this.startedAt = Instant.now();
    }

    /**
     * Mark the job as completed.
     *
     * @param result the result of the job execution
     */
    public void markAsCompleted(Map<String, Object> result) {
        this.status = JobStatus.COMPLETED;
        this.result = result;
        this.completedAt = Instant.now();
    }

    /**
     * Mark the job as failed.
     *
     * @param errorMessage the error message describing the failure
     */
    public void markAsFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    /**
     * Move the job to dead letter queue.
     */
    public void moveToDeadLetter() {
        this.status = JobStatus.DEAD_LETTER;
        this.completedAt = Instant.now();
    }

    /**
     * Cancel the job.
     */
    public void cancel() {
        if (isCancellable()) {
            this.status = JobStatus.CANCELLED;
            this.completedAt = Instant.now();
        }
    }
}
