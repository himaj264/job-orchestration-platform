package com.jobplatform.orchestrator.dto;

import com.jobplatform.orchestrator.model.JobType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Data Transfer Object for job creation requests.
 *
 * This DTO captures all the information needed to create a new job.
 * Validation annotations ensure that incoming requests meet the required criteria.
 *
 * @author Hima Kammachi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {

    /**
     * Human-readable name for the job.
     * Used for identification and logging purposes.
     */
    @NotBlank(message = "Job name is required")
    @Size(min = 1, max = 255, message = "Job name must be between 1 and 255 characters")
    private String name;

    /**
     * The type of job to execute.
     * Determines which processing logic will be applied.
     */
    @NotNull(message = "Job type is required")
    private JobType type;

    /**
     * Job execution priority (1-10).
     * Higher priority jobs are processed first.
     * Default is 5 if not specified.
     */
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority must be at most 10")
    @Builder.Default
    private Integer priority = 5;

    /**
     * The payload containing job-specific data.
     * This is stored as JSONB and passed to the worker for processing.
     */
    private Map<String, Object> payload;

    /**
     * Maximum number of retry attempts for this job.
     * Default is 3 if not specified.
     */
    @Min(value = 0, message = "Max retries must be at least 0")
    @Max(value = 10, message = "Max retries must be at most 10")
    @Builder.Default
    private Integer maxRetries = 3;
}
