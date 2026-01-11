package com.jobplatform.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for job statistics response.
 *
 * Provides an overview of job counts by status for monitoring
 * and dashboard purposes.
 *
 * @author Hima Kammachi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatsResponse {

    /**
     * Total number of jobs in the system.
     */
    private Long total;

    /**
     * Number of jobs waiting to be processed.
     */
    private Long pending;

    /**
     * Number of jobs currently being processed.
     */
    private Long running;

    /**
     * Number of successfully completed jobs.
     */
    private Long completed;

    /**
     * Number of jobs that failed (may be retried).
     */
    private Long failed;

    /**
     * Number of jobs that were cancelled.
     */
    private Long cancelled;

    /**
     * Number of jobs in the dead letter queue.
     */
    private Long deadLetter;

    /**
     * Average execution time in milliseconds for completed jobs.
     */
    private Double avgExecutionTimeMs;
}
