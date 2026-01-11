package com.jobplatform.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Worker Service Application
 *
 * This is the main entry point for the Job Worker Service.
 * It consumes jobs from Kafka, executes them, and publishes
 * status updates back to the orchestrator.
 *
 * Key Responsibilities:
 * - Consume job execution requests from Kafka
 * - Check idempotency using Redis
 * - Execute jobs with retry logic
 * - Publish status updates to Kafka
 * - Handle failed jobs and DLQ
 *
 * @author Hima Kammachi
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
