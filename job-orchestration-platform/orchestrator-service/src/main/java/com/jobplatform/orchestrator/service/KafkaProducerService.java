package com.jobplatform.orchestrator.service;

import com.jobplatform.orchestrator.dto.JobEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing job events to Kafka topics.
 *
 * Handles all Kafka message production with proper error handling
 * and logging for debugging and monitoring purposes.
 *
 * @author Hima Kammachi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, JobEvent> kafkaTemplate;

    @Value("${job.kafka.topics.requests:job-requests}")
    private String jobRequestsTopic;

    @Value("${job.kafka.topics.status:job-status}")
    private String jobStatusTopic;

    @Value("${job.kafka.topics.dlq:job-dlq}")
    private String jobDlqTopic;

    /**
     * Publish a job request event for worker processing.
     *
     * Uses the job ID as the message key to ensure all events
     * for the same job go to the same partition (ordering guarantee).
     *
     * @param event the job event to publish
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<SendResult<String, JobEvent>> publishJobRequest(JobEvent event) {
        String key = event.getJobId().toString();

        log.info("Publishing job request: job-id={}, type={}, priority={}",
                event.getJobId(), event.getType(), event.getPriority());

        CompletableFuture<SendResult<String, JobEvent>> future =
                kafkaTemplate.send(jobRequestsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish job request: job-id={}, error={}",
                        event.getJobId(), ex.getMessage(), ex);
            } else {
                log.debug("Job request published successfully: job-id={}, partition={}, offset={}",
                        event.getJobId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        return future;
    }

    /**
     * Publish a job status update event.
     *
     * Called by workers to update job status after processing.
     *
     * @param event the status update event
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<SendResult<String, JobEvent>> publishStatusUpdate(JobEvent event) {
        String key = event.getJobId().toString();

        log.info("Publishing status update: job-id={}, status={}, event-type={}",
                event.getJobId(), event.getStatus(), event.getEventType());

        CompletableFuture<SendResult<String, JobEvent>> future =
                kafkaTemplate.send(jobStatusTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish status update: job-id={}, error={}",
                        event.getJobId(), ex.getMessage(), ex);
            } else {
                log.debug("Status update published: job-id={}, partition={}, offset={}",
                        event.getJobId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        return future;
    }

    /**
     * Publish a job to the Dead Letter Queue.
     *
     * Called when a job exhausts all retry attempts and cannot be processed.
     *
     * @param event the failed job event
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<SendResult<String, JobEvent>> publishToDeadLetterQueue(JobEvent event) {
        String key = event.getJobId().toString();

        log.warn("Publishing to DLQ: job-id={}, error={}, retry-count={}",
                event.getJobId(), event.getErrorMessage(), event.getRetryCount());

        CompletableFuture<SendResult<String, JobEvent>> future =
                kafkaTemplate.send(jobDlqTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish to DLQ: job-id={}, error={}",
                        event.getJobId(), ex.getMessage(), ex);
            } else {
                log.info("Job moved to DLQ: job-id={}, partition={}, offset={}",
                        event.getJobId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        return future;
    }

    /**
     * Send a message to a specific topic.
     *
     * Generic method for custom topic publishing needs.
     *
     * @param topic the target topic
     * @param key   the message key
     * @param event the event to send
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<SendResult<String, JobEvent>> sendToTopic(
            String topic, String key, JobEvent event) {

        log.debug("Sending to topic {}: key={}, event-type={}",
                topic, key, event.getEventType());

        return kafkaTemplate.send(topic, key, event);
    }
}
