package com.jobplatform.orchestrator.model;

/**
 * Enumeration representing all possible states of a job.
 *
 * State Transitions:
 * <pre>
 * PENDING ──────► RUNNING ──────► COMPLETED
 *                    │
 *                    │ (on failure)
 *                    ▼
 *                 FAILED ──────► PENDING (retry)
 *                    │
 *                    │ (max retries exceeded)
 *                    ▼
 *              DEAD_LETTER
 *
 * Any state ─────► CANCELLED (manual cancellation)
 * </pre>
 *
 * @author Hima Kammachi
 */
public enum JobStatus {

    /**
     * Job has been created and is waiting to be processed.
     * This is the initial state for all jobs.
     */
    PENDING,

    /**
     * Job is currently being executed by a worker.
     * Transitions from PENDING when a worker picks up the job.
     */
    RUNNING,

    /**
     * Job has been successfully completed.
     * This is a terminal state - no further transitions allowed.
     */
    COMPLETED,

    /**
     * Job execution failed but may be retried.
     * Can transition back to PENDING if retry attempts remain,
     * or to DEAD_LETTER if max retries exceeded.
     */
    FAILED,

    /**
     * Job was manually cancelled by user request.
     * This is a terminal state - no further transitions allowed.
     */
    CANCELLED,

    /**
     * Job has exhausted all retry attempts and cannot be processed.
     * Requires manual intervention. This is a terminal state.
     */
    DEAD_LETTER;

    /**
     * Check if this status represents a terminal state.
     * Terminal states cannot transition to any other state.
     *
     * @return true if this is a terminal state
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == DEAD_LETTER;
    }

    /**
     * Check if the job can be retried from this status.
     *
     * @return true if the job can be retried
     */
    public boolean isRetryable() {
        return this == FAILED;
    }

    /**
     * Check if the job is currently being processed.
     *
     * @return true if the job is in an active processing state
     */
    public boolean isActive() {
        return this == RUNNING;
    }
}
