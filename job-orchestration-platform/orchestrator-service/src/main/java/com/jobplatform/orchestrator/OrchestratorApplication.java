package com.jobplatform.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Orchestrator Service Application
 *
 * This is the main entry point for the Job Orchestration Service.
 * It manages job lifecycle, provides REST APIs for job submission,
 * and publishes job events to Kafka for worker processing.
 *
 * Key Responsibilities:
 * - Accept job submissions via REST API
 * - Persist job metadata to PostgreSQL
 * - Publish job execution events to Kafka
 * - Consume status updates from workers
 * - Provide job status and statistics endpoints
 *
 * @author Hima Kammachi
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
public class OrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApplication.class, args);
    }
}
