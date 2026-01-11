package com.jobplatform.orchestrator.consumer;

import com.jobplatform.orchestrator.dto.JobEvent;
import com.jobplatform.orchestrator.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for job status updates.
 *
 * Listens to the job-status topic for updates from worker services.
 * Updates job status in the database based on received events.
 *
 * @author Hima Kammachi
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobStatusConsumer {

    private final JobService jobService;

    /**
     * Consume status update events from workers.
     *
     * This listener receives events published by workers when jobs
     * start, complete, or fail. It updates the job status in the database.
     *
     * @param record         the Kafka consumer record
     * @param acknowledgment manual acknowledgment for at-least-once delivery
     */
    @KafkaListener(
            topics = "${job.kafka.topics.status:job-status}",
            groupId = "orchestrator-status-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStatusUpdate(
            ConsumerRecord<String, JobEvent> record,
            Acknowledgment acknowledgment) {

        JobEvent event = record.value();

        log.info("Received status update: job-id={}, status={}, event-type={}, partition={}, offset={}",
                event.getJobId(),
                event.getStatus(),
                event.getEventType(),
                record.partition(),
                record.offset());

        try {
            // Process the status update
            jobService.updateJobStatus(event);

            // Acknowledge successful processing
            acknowledgment.acknowledge();

            log.debug("Status update processed successfully: job-id={}", event.getJobId());

        } catch (Exception e) {
            log.error("Error processing status update: job-id={}, error={}",
                    event.getJobId(), e.getMessage(), e);

            // Don't acknowledge - message will be redelivered
            // In production, you might want more sophisticated error handling
            throw e;
        }
    }
}
