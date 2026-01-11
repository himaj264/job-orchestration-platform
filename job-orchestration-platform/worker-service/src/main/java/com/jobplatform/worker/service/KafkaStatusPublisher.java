package com.jobplatform.worker.service;

import com.jobplatform.worker.dto.JobEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing job status updates to Kafka.
 *
 * Workers use this service to report job progress back to the orchestrator.
 *
 * @author Hima Kammachi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaStatusPublisher {

    private final KafkaTemplate<String, JobEvent> kafkaTemplate;

    @Value("${job.kafka.topics.status:job-status}")
    private String statusTopic;

    @Value("${job.kafka.topics.dlq:job-dlq}")
    private String dlqTopic;

    /**
     * Publish a status update event.
     *
     * @param event the status event to publish
     * @return CompletableFuture with send result
     */
    public CompletableFuture<SendResult<String, JobEvent>> publishStatus(JobEvent event) {
        String key = event.getJobId().toString();

        log.info("Publishing status update: job-id={}, status={}, event-type={}",
                event.getJobId(), event.getStatus(), event.getEventType());

        CompletableFuture<SendResult<String, JobEvent>> future =
                kafkaTemplate.send(statusTopic, key, event);

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
     * Publish a job started event.
     *
     * @param event    the original job event
     * @param workerId the worker processing the job
     */
    public void publishStarted(JobEvent event, String workerId) {
        JobEvent statusEvent = event.toStatusUpdate(JobEvent.EventType.JOB_STARTED, workerId);
        publishStatus(statusEvent);
    }

    /**
     * Publish a job completed event.
     *
     * @param event    the job event with results
     * @param workerId the worker that processed the job
     */
    public void publishCompleted(JobEvent event, String workerId) {
        JobEvent statusEvent = event.toStatusUpdate(JobEvent.EventType.JOB_COMPLETED, workerId);
        publishStatus(statusEvent);
    }

    /**
     * Publish a job failed event.
     *
     * @param event        the job event
     * @param workerId     the worker that processed the job
     * @param errorMessage the error message
     */
    public void publishFailed(JobEvent event, String workerId, String errorMessage) {
        event.setErrorMessage(errorMessage);
        if (event.getRetryCount() == null) {
            event.setRetryCount(0);
        }
        event.setRetryCount(event.getRetryCount() + 1);

        JobEvent statusEvent = event.toStatusUpdate(JobEvent.EventType.JOB_FAILED, workerId);
        publishStatus(statusEvent);
    }

    /**
     * Publish to Dead Letter Queue.
     *
     * @param event    the failed job event
     * @param workerId the worker ID
     */
    public void publishToDeadLetterQueue(JobEvent event, String workerId) {
        String key = event.getJobId().toString();

        log.warn("Publishing to DLQ: job-id={}, error={}, retry-count={}",
                event.getJobId(), event.getErrorMessage(), event.getRetryCount());

        JobEvent dlqEvent = event.toStatusUpdate(JobEvent.EventType.JOB_DEAD_LETTER, workerId);

        kafkaTemplate.send(dlqTopic, key, dlqEvent).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish to DLQ: job-id={}, error={}",
                        event.getJobId(), ex.getMessage(), ex);
            } else {
                log.info("Job moved to DLQ: job-id={}", event.getJobId());
            }
        });

        // Also notify orchestrator
        publishStatus(dlqEvent);
    }
}
