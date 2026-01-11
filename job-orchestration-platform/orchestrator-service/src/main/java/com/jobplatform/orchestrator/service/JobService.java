package com.jobplatform.orchestrator.service;

import com.jobplatform.orchestrator.dto.*;
import com.jobplatform.orchestrator.exception.JobNotFoundException;
import com.jobplatform.orchestrator.exception.JobOperationException;
import com.jobplatform.orchestrator.model.Job;
import com.jobplatform.orchestrator.model.JobStatus;
import com.jobplatform.orchestrator.model.JobType;
import com.jobplatform.orchestrator.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service layer for job management operations.
 *
 * Handles all business logic for job CRUD operations,
 * status management, and event publishing.
 *
 * @author Hima Kammachi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Create a new job and publish it for processing.
     *
     * @param request the job creation request
     * @return the created job response
     */
    @Transactional
    public JobResponse createJob(JobRequest request) {
        log.info("Creating new job: name={}, type={}, priority={}",
                request.getName(), request.getType(), request.getPriority());

        // Build and save the job entity
        Job job = Job.builder()
                .name(request.getName())
                .type(request.getType())
                .priority(request.getPriority())
                .payload(request.getPayload())
                .maxRetries(request.getMaxRetries())
                .status(JobStatus.PENDING)
                .build();

        Job savedJob = jobRepository.save(job);
        log.info("Job created: job-id={}", savedJob.getId());

        // Publish job event to Kafka for worker processing
        JobEvent event = JobEvent.fromJob(savedJob);
        kafkaProducerService.publishJobRequest(event);

        return JobResponse.fromEntity(savedJob);
    }

    /**
     * Get a job by its ID.
     *
     * @param jobId the job ID
     * @return the job response
     * @throws JobNotFoundException if job not found
     */
    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        Job job = findJobOrThrow(jobId);
        return JobResponse.fromEntity(job);
    }

    /**
     * Get all jobs with pagination.
     *
     * @param pageable pagination parameters
     * @return page of job responses
     */
    @Transactional(readOnly = true)
    public Page<JobResponse> getAllJobs(Pageable pageable) {
        return jobRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(JobResponse::fromEntity);
    }

    /**
     * Get jobs filtered by status.
     *
     * @param status   the status to filter by
     * @param pageable pagination parameters
     * @return page of matching job responses
     */
    @Transactional(readOnly = true)
    public Page<JobResponse> getJobsByStatus(JobStatus status, Pageable pageable) {
        return jobRepository.findByStatus(status, pageable)
                .map(JobResponse::fromEntity);
    }

    /**
     * Get jobs filtered by type.
     *
     * @param type     the type to filter by
     * @param pageable pagination parameters
     * @return page of matching job responses
     */
    @Transactional(readOnly = true)
    public Page<JobResponse> getJobsByType(JobType type, Pageable pageable) {
        return jobRepository.findByType(type, pageable)
                .map(JobResponse::fromEntity);
    }

    /**
     * Cancel a pending job.
     *
     * @param jobId the job ID to cancel
     * @return the updated job response
     * @throws JobNotFoundException   if job not found
     * @throws JobOperationException if job cannot be cancelled
     */
    @Transactional
    public JobResponse cancelJob(UUID jobId) {
        Job job = findJobOrThrow(jobId);

        if (!job.isCancellable()) {
            throw new JobOperationException(
                    "Job cannot be cancelled in status: " + job.getStatus());
        }

        job.cancel();
        Job savedJob = jobRepository.save(job);

        log.info("Job cancelled: job-id={}", jobId);

        // Publish cancellation event
        JobEvent event = JobEvent.statusUpdate(savedJob, JobEvent.EventType.JOB_CANCELLED);
        kafkaProducerService.publishStatusUpdate(event);

        return JobResponse.fromEntity(savedJob);
    }

    /**
     * Update job status from worker callback.
     *
     * @param event the status update event from worker
     */
    @Transactional
    public void updateJobStatus(JobEvent event) {
        log.info("Updating job status: job-id={}, status={}, event-type={}",
                event.getJobId(), event.getStatus(), event.getEventType());

        Job job = findJobOrThrow(event.getJobId());

        // Update job fields based on event type
        switch (event.getEventType()) {
            case JOB_STARTED:
                job.markAsRunning(event.getWorkerId());
                break;

            case JOB_COMPLETED:
                job.markAsCompleted(event.getResult());
                break;

            case JOB_FAILED:
                job.markAsFailed(event.getErrorMessage());
                job.setRetryCount(event.getRetryCount());

                // Check if job should be retried or moved to DLQ
                if (job.canRetry()) {
                    log.info("Job will be retried: job-id={}, retry-count={}/{}",
                            job.getId(), job.getRetryCount(), job.getMaxRetries());
                    scheduleRetry(job);
                } else {
                    log.warn("Job exhausted retries, moving to DLQ: job-id={}",
                            job.getId());
                    job.moveToDeadLetter();
                    JobEvent dlqEvent = JobEvent.statusUpdate(job, JobEvent.EventType.JOB_DEAD_LETTER);
                    kafkaProducerService.publishToDeadLetterQueue(dlqEvent);
                }
                break;

            case JOB_DEAD_LETTER:
                job.moveToDeadLetter();
                break;

            default:
                log.warn("Unknown event type: {}", event.getEventType());
        }

        jobRepository.save(job);
    }

    /**
     * Schedule a job for retry.
     *
     * @param job the job to retry
     */
    private void scheduleRetry(Job job) {
        job.setStatus(JobStatus.PENDING);
        job.incrementRetryCount();
        jobRepository.save(job);

        // Republish job for processing
        JobEvent event = JobEvent.fromJob(job);
        event.setEventType(JobEvent.EventType.JOB_RETRY);
        kafkaProducerService.publishJobRequest(event);
    }

    /**
     * Get job statistics.
     *
     * @return job statistics response
     */
    @Transactional(readOnly = true)
    public JobStatsResponse getJobStatistics() {
        return JobStatsResponse.builder()
                .total(jobRepository.count())
                .pending(jobRepository.countByStatus(JobStatus.PENDING))
                .running(jobRepository.countByStatus(JobStatus.RUNNING))
                .completed(jobRepository.countByStatus(JobStatus.COMPLETED))
                .failed(jobRepository.countByStatus(JobStatus.FAILED))
                .cancelled(jobRepository.countByStatus(JobStatus.CANCELLED))
                .deadLetter(jobRepository.countByStatus(JobStatus.DEAD_LETTER))
                .avgExecutionTimeMs(jobRepository.calculateAverageExecutionTime().orElse(0.0))
                .build();
    }

    /**
     * Search jobs by name.
     *
     * @param name     search term
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Transactional(readOnly = true)
    public Page<JobResponse> searchJobsByName(String name, Pageable pageable) {
        return jobRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(JobResponse::fromEntity);
    }

    /**
     * Find job or throw exception.
     *
     * @param jobId the job ID
     * @return the job entity
     * @throws JobNotFoundException if not found
     */
    private Job findJobOrThrow(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + jobId));
    }
}
