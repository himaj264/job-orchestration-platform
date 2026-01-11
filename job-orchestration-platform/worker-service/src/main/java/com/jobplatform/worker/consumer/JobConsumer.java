package com.jobplatform.worker.consumer;

import com.jobplatform.worker.dto.JobEvent;
import com.jobplatform.worker.service.IdempotencyService;
import com.jobplatform.worker.service.JobExecutorService;
import com.jobplatform.worker.service.JobExecutorService.JobExecutionException;
import com.jobplatform.worker.service.KafkaStatusPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for job processing.
 *
 * Consumes job requests from Kafka, executes them with idempotency checks,
 * and publishes status updates back to the orchestrator.
 *
 * Key features:
 * - Idempotency checks using Redis (prevents duplicate execution)
 * - Status updates published to orchestrator
 * - Manual acknowledgment for at-least-once delivery
 * - Error handling with retry support
 *
 * @author Hima Kammachi
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobConsumer {

    private final IdempotencyService idempotencyService;
    private final JobExecutorService jobExecutorService;
    private final KafkaStatusPublisher statusPublisher;

    /**
     * Consume and process job requests from Kafka.
     *
     * Processing flow:
     * 1. Receive job event from Kafka
     * 2. Check idempotency (skip if already processed)
     * 3. Publish "started" status
     * 4. Execute the job
     * 5. Publish "completed" or "failed" status
     * 6. Acknowledge the message
     *
     * @param record         the Kafka consumer record
     * @param acknowledgment manual acknowledgment
     */
    @KafkaListener(
            topics = "${job.kafka.topics.requests:job-requests}",
            groupId = "${spring.kafka.consumer.group-id:job-worker-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeJob(
            ConsumerRecord<String, JobEvent> record,
            Acknowledgment acknowledgment) {

        JobEvent event = record.value();
        String workerId = jobExecutorService.getWorkerId();

        log.info("Received job: job-id={}, type={}, partition={}, offset={}",
                event.getJobId(),
                event.getType(),
                record.partition(),
                record.offset());

        try {
            // Step 1: Idempotency check
            if (!idempotencyService.tryAcquire(event.getJobId())) {
                log.info("Duplicate job detected, skipping: job-id={}", event.getJobId());
                acknowledgment.acknowledge();
                return;
            }

            // Step 2: Publish "started" status
            statusPublisher.publishStarted(event, workerId);

            // Step 3: Execute the job
            Map<String, Object> result = jobExecutorService.execute(event);

            // Step 4: Update event with result
            event.setResult(result);

            // Step 5: Mark as completed in idempotency store
            idempotencyService.markCompleted(event.getJobId());

            // Step 6: Publish "completed" status
            statusPublisher.publishCompleted(event, workerId);

            log.info("Job completed successfully: job-id={}", event.getJobId());

        } catch (JobExecutionException e) {
            handleJobFailure(event, workerId, e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error processing job: job-id={}, error={}",
                    event.getJobId(), e.getMessage(), e);
            handleJobFailure(event, workerId, "Unexpected error: " + e.getMessage());
        } finally {
            // Always acknowledge to prevent infinite redelivery
            // The orchestrator handles retries via status updates
            acknowledgment.acknowledge();
        }
    }

    /**
     * Handle job execution failure.
     *
     * Determines whether to retry or move to DLQ based on retry count.
     *
     * @param event        the failed job event
     * @param workerId     the worker ID
     * @param errorMessage the error message
     */
    private void handleJobFailure(JobEvent event, String workerId, String errorMessage) {
        log.error("Job execution failed: job-id={}, error={}", event.getJobId(), errorMessage);

        int currentRetry = event.getRetryCount() != null ? event.getRetryCount() : 0;
        int maxRetries = event.getMaxRetries() != null ? event.getMaxRetries() : 3;

        if (currentRetry < maxRetries) {
            // Allow retry - release idempotency key
            idempotencyService.markFailed(event.getJobId(), true);

            // Publish failed status (orchestrator will handle retry)
            statusPublisher.publishFailed(event, workerId, errorMessage);

            log.info("Job scheduled for retry: job-id={}, retry={}/{}",
                    event.getJobId(), currentRetry + 1, maxRetries);
        } else {
            // Max retries exceeded - move to DLQ
            idempotencyService.markFailed(event.getJobId(), false);

            event.setRetryCount(currentRetry);
            event.setErrorMessage(errorMessage);

            statusPublisher.publishToDeadLetterQueue(event, workerId);

            log.warn("Job moved to DLQ after {} retries: job-id={}",
                    maxRetries, event.getJobId());
        }
    }
}
