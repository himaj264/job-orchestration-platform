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
 * Data Transfer Object for job responses.
 *
 * This DTO is returned to clients when querying job information.
 * It includes all relevant job details including status, timing, and results.
 *
 * @author Hima Kammachi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {

    private UUID id;
    private String name;
    private JobType type;
    private JobStatus status;
    private Integer priority;
    private Map<String, Object> payload;
    private Map<String, Object> result;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant startedAt;
    private Instant completedAt;
    private String workerId;

    /**
     * Calculated field: execution duration in milliseconds.
     * Only available for completed/failed jobs.
     */
    private Long executionTimeMs;

    /**
     * Convert a Job entity to a JobResponse DTO.
     *
     * @param job the job entity
     * @return the response DTO
     */
    public static JobResponse fromEntity(Job job) {
        Long executionTimeMs = null;
        if (job.getStartedAt() != null && job.getCompletedAt() != null) {
            executionTimeMs = job.getCompletedAt().toEpochMilli() - job.getStartedAt().toEpochMilli();
        }

        return JobResponse.builder()
                .id(job.getId())
                .name(job.getName())
                .type(job.getType())
                .status(job.getStatus())
                .priority(job.getPriority())
                .payload(job.getPayload())
                .result(job.getResult())
                .errorMessage(job.getErrorMessage())
                .retryCount(job.getRetryCount())
                .maxRetries(job.getMaxRetries())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .workerId(job.getWorkerId())
                .executionTimeMs(executionTimeMs)
                .build();
    }
}
