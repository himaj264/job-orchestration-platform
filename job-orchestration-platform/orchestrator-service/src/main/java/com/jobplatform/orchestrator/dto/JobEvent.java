package com.jobplatform.orchestrator.dto;

import com.jobplatform.orchestrator.model.Job;
import com.jobplatform.orchestrator.model.JobStatus;
import com.jobplatform.orchestrator.model.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Event object published to Kafka for job processing.
 *
 * This DTO represents a job event that is sent to Kafka topics.
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
     * Type of event (JOB_CREATED, STATUS_UPDATE, etc.).
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
     * Create a job creation event from a Job entity.
     *
     * @param job the job entity
     * @return the job event
     */
    public static JobEvent fromJob(Job job) {
        return JobEvent.builder()
                .jobId(job.getId())
                .name(job.getName())
                .type(job.getType())
                .status(job.getStatus())
                .priority(job.getPriority())
                .payload(job.getPayload())
                .retryCount(job.getRetryCount())
                .maxRetries(job.getMaxRetries())
                .timestamp(Instant.now())
                .eventType(EventType.JOB_CREATED)
                .build();
    }

    /**
     * Create a status update event.
     *
     * @param job       the job entity
     * @param eventType the type of event
     * @return the job event
     */
    public static JobEvent statusUpdate(Job job, EventType eventType) {
        return JobEvent.builder()
                .jobId(job.getId())
                .name(job.getName())
                .type(job.getType())
                .status(job.getStatus())
                .priority(job.getPriority())
                .result(job.getResult())
                .errorMessage(job.getErrorMessage())
                .retryCount(job.getRetryCount())
                .maxRetries(job.getMaxRetries())
                .workerId(job.getWorkerId())
                .timestamp(Instant.now())
                .eventType(eventType)
                .build();
    }
}
