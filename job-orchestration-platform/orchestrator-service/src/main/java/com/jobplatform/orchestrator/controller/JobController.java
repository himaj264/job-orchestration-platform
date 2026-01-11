package com.jobplatform.orchestrator.controller;

import com.jobplatform.orchestrator.dto.JobRequest;
import com.jobplatform.orchestrator.dto.JobResponse;
import com.jobplatform.orchestrator.dto.JobStatsResponse;
import com.jobplatform.orchestrator.model.JobStatus;
import com.jobplatform.orchestrator.model.JobType;
import com.jobplatform.orchestrator.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Job Management API.
 *
 * Provides endpoints for:
 * - Creating new jobs
 * - Querying job status
 * - Cancelling jobs
 * - Viewing job statistics
 *
 * @author Hima Kammachi
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

    private final JobService jobService;

    /**
     * Create a new job.
     *
     * POST /api/jobs
     *
     * @param request the job creation request
     * @return the created job with status 201
     */
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) {
        log.info("REST request to create job: name={}, type={}",
                request.getName(), request.getType());

        JobResponse response = jobService.createJob(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a job by ID.
     *
     * GET /api/jobs/{jobId}
     *
     * @param jobId the job ID
     * @return the job details
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID jobId) {
        log.debug("REST request to get job: job-id={}", jobId);

        JobResponse response = jobService.getJob(jobId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all jobs with optional filtering.
     *
     * GET /api/jobs?status=PENDING&type=PROCESS_DATA&page=0&size=20
     *
     * @param status optional status filter
     * @param type   optional type filter
     * @param page   page number (0-based)
     * @param size   page size
     * @return page of jobs
     */
    @GetMapping
    public ResponseEntity<Page<JobResponse>> getAllJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) JobType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("REST request to get jobs: status={}, type={}, page={}, size={}",
                status, type, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<JobResponse> response;
        if (status != null) {
            response = jobService.getJobsByStatus(status, pageable);
        } else if (type != null) {
            response = jobService.getJobsByType(type, pageable);
        } else {
            response = jobService.getAllJobs(pageable);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Search jobs by name.
     *
     * GET /api/jobs/search?name=test&page=0&size=20
     *
     * @param name search term
     * @param page page number
     * @param size page size
     * @return page of matching jobs
     */
    @GetMapping("/search")
    public ResponseEntity<Page<JobResponse>> searchJobs(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("REST request to search jobs: name={}", name);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<JobResponse> response = jobService.searchJobsByName(name, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a pending job.
     *
     * DELETE /api/jobs/{jobId}
     *
     * @param jobId the job ID to cancel
     * @return the cancelled job
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<JobResponse> cancelJob(@PathVariable UUID jobId) {
        log.info("REST request to cancel job: job-id={}", jobId);

        JobResponse response = jobService.cancelJob(jobId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get job statistics.
     *
     * GET /api/jobs/stats
     *
     * @return job statistics summary
     */
    @GetMapping("/stats")
    public ResponseEntity<JobStatsResponse> getJobStatistics() {
        log.debug("REST request to get job statistics");

        JobStatsResponse response = jobService.getJobStatistics();

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     *
     * GET /api/jobs/health
     *
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
