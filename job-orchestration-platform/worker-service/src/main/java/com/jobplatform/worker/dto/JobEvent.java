package com.jobplatform.worker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Event object received from Kafka for job processing.
 *
 * This DTO represents a job event consumed from Kafka topics.
 * It contains all the information workers need to process a job.
 *
 * @author Hima Kammachi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEvent {

    /**
     * Unique identifier for the job.
     */
    private UUID jobId;

    /**
     * Human-readable job name.
     */
    private String name;

    /**
     * Type of job to execute.
     */
    private JobType type;

    /**
     * Current status of the job.
     */
    private JobStatus status;

    /**
     * Job priority (1-10).
     */
    private Integer priority;

    /**
     * Job payload data.
     */
    private Map<String, Object> payload;

    /**
     * Job result (for status updates).
     */
    private Map<String, Object> result;

    /**
     * Error message (for failed jobs).
     */
    private String errorMessage;

    /**
     * Current retry count.
     */
    private Integer retryCount;

    /**
     * Maximum allowed retries.
     */
    private Integer maxRetries;

    /**
     * ID of the worker processing this job.
     */
    private String workerId;

    /**
     * Timestamp when this event was created.
     */
    private Instant timestamp;

    /**
     * Type of event.
     */
    private EventType eventType;

    /**
     * Enumeration of possible event types.
     */
    public enum EventType {
        JOB_CREATED,
        JOB_STARTED,
        JOB_COMPLETED,
        JOB_FAILED,
        JOB_RETRY,
        JOB_DEAD_LETTER,
        JOB_CANCELLED
    }

    /**
     * Enumeration representing job status.
     */
    public enum JobStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED,
        DEAD_LETTER
    }

    /**
     * Enumeration representing job types.
     */
    public enum JobType {
        PROCESS_DATA,
        SEND_EMAIL,
        GENERATE_REPORT,
        SYNC_DATA
    }

    /**
     * Create a status update event.
     *
     * @param eventType the type of event
     * @param workerId  the worker ID
     * @return updated event
     */
    public JobEvent toStatusUpdate(EventType eventType, String workerId) {
        return JobEvent.builder()
                .jobId(this.jobId)
                .name(this.name)
                .type(this.type)
                .status(mapEventTypeToStatus(eventType))
                .priority(this.priority)
                .payload(this.payload)
                .result(this.result)
                .errorMessage(this.errorMessage)
                .retryCount(this.retryCount)
                .maxRetries(this.maxRetries)
                .workerId(workerId)
                .timestamp(Instant.now())
                .eventType(eventType)
                .build();
    }

    /**
     * Map event type to job status.
     */
    private JobStatus mapEventTypeToStatus(EventType eventType) {
        return switch (eventType) {
            case JOB_STARTED -> JobStatus.RUNNING;
            case JOB_COMPLETED -> JobStatus.COMPLETED;
            case JOB_FAILED -> JobStatus.FAILED;
            case JOB_DEAD_LETTER -> JobStatus.DEAD_LETTER;
            case JOB_CANCELLED -> JobStatus.CANCELLED;
            default -> this.status;
        };
    }
}
