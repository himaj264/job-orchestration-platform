package com.jobplatform.worker.service;

import com.jobplatform.worker.dto.JobEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Service for executing jobs.
 *
 * This service contains the actual job processing logic for different job types.
 * In a real application, this would integrate with external systems, databases,
 * APIs, etc.
 *
 * @author Hima Kammachi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutorService {

    @Value("${worker.id:worker-default}")
    private String workerId;

    private final Random random = new Random();

    /**
     * Execute a job based on its type.
     *
     * @param event the job event to execute
     * @return the execution result
     * @throws JobExecutionException if execution fails
     */
    public Map<String, Object> execute(JobEvent event) throws JobExecutionException {
        log.info("Executing job: job-id={}, type={}, name={}",
                event.getJobId(), event.getType(), event.getName());

        try {
            // Route to appropriate handler based on job type
            Map<String, Object> result = switch (event.getType()) {
                case PROCESS_DATA -> executeProcessData(event);
                case SEND_EMAIL -> executeSendEmail(event);
                case GENERATE_REPORT -> executeGenerateReport(event);
                case SYNC_DATA -> executeSyncData(event);
            };

            log.info("Job executed successfully: job-id={}", event.getJobId());
            return result;

        } catch (Exception e) {
            log.error("Job execution failed: job-id={}, error={}",
                    event.getJobId(), e.getMessage(), e);
            throw new JobExecutionException("Job execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a data processing job.
     *
     * Simulates data transformation, analysis, or manipulation.
     */
    private Map<String, Object> executeProcessData(JobEvent event) throws InterruptedException {
        log.debug("Processing data job: job-id={}", event.getJobId());

        // Simulate processing time (1-3 seconds)
        simulateWork(1000, 3000);

        // Simulate occasional failures (10% chance)
        simulateRandomFailure(0.1, "Data processing error: invalid format");

        Map<String, Object> result = new HashMap<>();
        result.put("recordsProcessed", random.nextInt(1000) + 100);
        result.put("processingTimeMs", System.currentTimeMillis());
        result.put("status", "success");

        return result;
    }

    /**
     * Execute an email sending job.
     *
     * Simulates sending an email notification.
     */
    private Map<String, Object> executeSendEmail(JobEvent event) throws InterruptedException {
        log.debug("Sending email job: job-id={}", event.getJobId());

        // Simulate email sending (500ms - 1.5s)
        simulateWork(500, 1500);

        // Simulate occasional failures (5% chance)
        simulateRandomFailure(0.05, "Email sending failed: SMTP connection error");

        Map<String, Object> payload = event.getPayload();
        String recipient = payload != null ? (String) payload.get("to") : "unknown";

        Map<String, Object> result = new HashMap<>();
        result.put("recipient", recipient);
        result.put("sentAt", System.currentTimeMillis());
        result.put("messageId", "MSG-" + event.getJobId().toString().substring(0, 8));
        result.put("status", "delivered");

        return result;
    }

    /**
     * Execute a report generation job.
     *
     * Simulates generating a report or analytics document.
     */
    private Map<String, Object> executeGenerateReport(JobEvent event) throws InterruptedException {
        log.debug("Generating report job: job-id={}", event.getJobId());

        // Simulate report generation (2-5 seconds)
        simulateWork(2000, 5000);

        // Simulate occasional failures (8% chance)
        simulateRandomFailure(0.08, "Report generation failed: insufficient data");

        Map<String, Object> result = new HashMap<>();
        result.put("reportId", "RPT-" + event.getJobId().toString().substring(0, 8));
        result.put("format", "PDF");
        result.put("pages", random.nextInt(50) + 5);
        result.put("generatedAt", System.currentTimeMillis());
        result.put("status", "completed");

        return result;
    }

    /**
     * Execute a data synchronization job.
     *
     * Simulates syncing data between systems.
     */
    private Map<String, Object> executeSyncData(JobEvent event) throws InterruptedException {
        log.debug("Syncing data job: job-id={}", event.getJobId());

        // Simulate sync operation (1.5-4 seconds)
        simulateWork(1500, 4000);

        // Simulate occasional failures (12% chance)
        simulateRandomFailure(0.12, "Data sync failed: connection timeout");

        Map<String, Object> result = new HashMap<>();
        result.put("recordsSynced", random.nextInt(500) + 50);
        result.put("conflicts", random.nextInt(5));
        result.put("syncedAt", System.currentTimeMillis());
        result.put("status", "synchronized");

        return result;
    }

    /**
     * Simulate work by sleeping for a random duration.
     */
    private void simulateWork(int minMs, int maxMs) throws InterruptedException {
        int duration = random.nextInt(maxMs - minMs) + minMs;
        TimeUnit.MILLISECONDS.sleep(duration);
    }

    /**
     * Simulate random failures based on probability.
     */
    private void simulateRandomFailure(double probability, String errorMessage)
            throws JobExecutionException {
        if (random.nextDouble() < probability) {
            throw new JobExecutionException(errorMessage);
        }
    }

    /**
     * Get the worker ID.
     */
    public String getWorkerId() {
        return workerId;
    }

    /**
     * Exception for job execution failures.
     */
    public static class JobExecutionException extends Exception {
        public JobExecutionException(String message) {
            super(message);
        }

        public JobExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
