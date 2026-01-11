package com.jobplatform.orchestrator.repository;

import com.jobplatform.orchestrator.model.Job;
import com.jobplatform.orchestrator.model.JobStatus;
import com.jobplatform.orchestrator.model.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Job entity persistence operations.
 *
 * Provides CRUD operations and custom queries for job management.
 * Uses Spring Data JPA for automatic implementation.
 *
 * @author Hima Kammachi
 */
@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    /**
     * Find all jobs with a specific status.
     *
     * @param status   the job status to filter by
     * @param pageable pagination information
     * @return page of jobs with the specified status
     */
    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    /**
     * Find all jobs with a specific type.
     *
     * @param type     the job type to filter by
     * @param pageable pagination information
     * @return page of jobs with the specified type
     */
    Page<Job> findByType(JobType type, Pageable pageable);

    /**
     * Find all jobs with a specific status and type.
     *
     * @param status   the job status to filter by
     * @param type     the job type to filter by
     * @param pageable pagination information
     * @return page of matching jobs
     */
    Page<Job> findByStatusAndType(JobStatus status, JobType type, Pageable pageable);

    /**
     * Find jobs by status ordered by priority (descending) and creation time.
     *
     * @param status the job status to filter by
     * @return list of jobs ordered by priority
     */
    List<Job> findByStatusOrderByPriorityDescCreatedAtAsc(JobStatus status);

    /**
     * Find jobs created within a time range.
     *
     * @param start    start of the time range
     * @param end      end of the time range
     * @param pageable pagination information
     * @return page of jobs created within the range
     */
    Page<Job> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * Count jobs by status.
     *
     * @param status the job status to count
     * @return number of jobs with the specified status
     */
    long countByStatus(JobStatus status);

    /**
     * Count jobs by type.
     *
     * @param type the job type to count
     * @return number of jobs with the specified type
     */
    long countByType(JobType type);

    /**
     * Find jobs assigned to a specific worker.
     *
     * @param workerId the worker ID
     * @param pageable pagination information
     * @return page of jobs assigned to the worker
     */
    Page<Job> findByWorkerId(String workerId, Pageable pageable);

    /**
     * Find failed jobs that can be retried.
     *
     * @return list of failed jobs with remaining retry attempts
     */
    @Query("SELECT j FROM Job j WHERE j.status = 'FAILED' AND j.retryCount < j.maxRetries")
    List<Job> findRetryableJobs();

    /**
     * Calculate average execution time for completed jobs.
     *
     * @return average execution time in milliseconds
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (j.completedAt - j.startedAt)) * 1000) " +
            "FROM Job j WHERE j.status = 'COMPLETED' AND j.startedAt IS NOT NULL AND j.completedAt IS NOT NULL")
    Optional<Double> calculateAverageExecutionTime();

    /**
     * Find jobs by name containing a search term (case-insensitive).
     *
     * @param name     the search term
     * @param pageable pagination information
     * @return page of matching jobs
     */
    Page<Job> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Delete old completed jobs for cleanup.
     *
     * @param beforeDate delete jobs completed before this date
     * @return number of deleted jobs
     */
    @Query("DELETE FROM Job j WHERE j.status IN ('COMPLETED', 'CANCELLED') AND j.completedAt < :beforeDate")
    int deleteOldCompletedJobs(@Param("beforeDate") Instant beforeDate);

    /**
     * Find all jobs, ordered by creation time (newest first).
     *
     * @param pageable pagination information
     * @return page of all jobs
     */
    Page<Job> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
