package com.jobplatform.orchestrator.model;

/**
 * Enumeration representing different types of jobs that can be processed.
 *
 * Each job type may have different processing logic in the worker service.
 * This allows for type-specific handling and routing of jobs.
 *
 * @author Hima Kammachi
 */
public enum JobType {

    /**
     * Generic data processing job.
     * Used for transforming, analyzing, or manipulating data.
     */
    PROCESS_DATA("Data Processing", "Process and transform data"),

    /**
     * Email sending job.
     * Used for sending notifications, alerts, or marketing emails.
     */
    SEND_EMAIL("Email Notification", "Send email to specified recipients"),

    /**
     * Report generation job.
     * Used for creating reports, summaries, or analytics documents.
     */
    GENERATE_REPORT("Report Generation", "Generate reports and analytics"),

    /**
     * Data synchronization job.
     * Used for syncing data between different systems or databases.
     */
    SYNC_DATA("Data Synchronization", "Synchronize data across systems");

    private final String displayName;
    private final String description;

    JobType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
