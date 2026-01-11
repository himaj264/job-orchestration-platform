package com.jobplatform.orchestrator.config;

import com.jobplatform.orchestrator.dto.JobEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for the Orchestrator Service.
 *
 * Configures Kafka producer and topic settings for job event publishing.
 * Includes optimizations for reliability and performance.
 *
 * @author Hima Kammachi
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${job.kafka.topics.requests:job-requests}")
    private String jobRequestsTopic;

    @Value("${job.kafka.topics.status:job-status}")
    private String jobStatusTopic;

    @Value("${job.kafka.topics.dlq:job-dlq}")
    private String jobDlqTopic;

    /**
     * Producer factory configuration for JobEvent messages.
     *
     * Configured with:
     * - Idempotent producer for exactly-once semantics
     * - Acks=all for durability
     * - Retries for transient failures
     */
    @Bean
    public ProducerFactory<String, JobEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Basic configuration
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Performance settings
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        // Timeout settings
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka template for sending JobEvent messages.
     */
    @Bean
    public KafkaTemplate<String, JobEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Topic for job execution requests.
     * Workers consume from this topic to process jobs.
     */
    @Bean
    public NewTopic jobRequestsTopic() {
        return TopicBuilder.name(jobRequestsTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .build();
    }

    /**
     * Topic for job status updates.
     * Workers publish status updates, orchestrator consumes them.
     */
    @Bean
    public NewTopic jobStatusTopic() {
        return TopicBuilder.name(jobStatusTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .build();
    }

    /**
     * Dead Letter Queue topic for failed jobs.
     * Jobs that exhaust all retries are moved here.
     */
    @Bean
    public NewTopic jobDlqTopic() {
        return TopicBuilder.name(jobDlqTopic)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days
                .build();
    }
}
