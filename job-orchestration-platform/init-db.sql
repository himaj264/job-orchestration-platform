-- ============================================================================
-- Event-Driven Distributed Job Orchestration Platform
-- Database Initialization Script
-- ============================================================================

-- Create extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- JOBS TABLE - Main job metadata storage
-- ============================================================================
CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority INTEGER DEFAULT 5,
    payload JSONB,
    result JSONB,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    worker_id VARCHAR(100),

    -- Constraints
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED', 'DEAD_LETTER')),
    CONSTRAINT valid_type CHECK (type IN ('PROCESS_DATA', 'SEND_EMAIL', 'GENERATE_REPORT', 'SYNC_DATA')),
    CONSTRAINT valid_priority CHECK (priority >= 1 AND priority <= 10)
);

-- ============================================================================
-- JOB EXECUTION HISTORY TABLE - Audit trail for job executions
-- ============================================================================
CREATE TABLE IF NOT EXISTS job_execution_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    message TEXT,
    worker_id VARCHAR(100),
    execution_time_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_history_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'RETRY', 'CANCELLED', 'DEAD_LETTER'))
);

-- ============================================================================
-- INDEXES - Optimize query performance
-- ============================================================================

-- Index for querying jobs by status (most common query)
CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status);

-- Index for querying jobs by type
CREATE INDEX IF NOT EXISTS idx_jobs_type ON jobs(type);

-- Index for querying jobs by creation time (pagination)
CREATE INDEX IF NOT EXISTS idx_jobs_created_at ON jobs(created_at DESC);

-- Index for querying jobs by priority (job scheduling)
CREATE INDEX IF NOT EXISTS idx_jobs_priority ON jobs(priority DESC, created_at ASC);

-- Composite index for status + created_at (filtered pagination)
CREATE INDEX IF NOT EXISTS idx_jobs_status_created ON jobs(status, created_at DESC);

-- Index for execution history lookups
CREATE INDEX IF NOT EXISTS idx_execution_history_job_id ON job_execution_history(job_id);

-- Index for execution history by time
CREATE INDEX IF NOT EXISTS idx_execution_history_created ON job_execution_history(created_at DESC);

-- ============================================================================
-- FUNCTIONS - Utility functions
-- ============================================================================

-- Function to automatically update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Trigger to auto-update updated_at on jobs table
DROP TRIGGER IF EXISTS update_jobs_updated_at ON jobs;
CREATE TRIGGER update_jobs_updated_at
    BEFORE UPDATE ON jobs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- VIEWS - Convenient data views
-- ============================================================================

-- View for job statistics
CREATE OR REPLACE VIEW job_statistics AS
SELECT
    COUNT(*) AS total_jobs,
    COUNT(*) FILTER (WHERE status = 'PENDING') AS pending_jobs,
    COUNT(*) FILTER (WHERE status = 'RUNNING') AS running_jobs,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed_jobs,
    COUNT(*) FILTER (WHERE status = 'FAILED') AS failed_jobs,
    COUNT(*) FILTER (WHERE status = 'DEAD_LETTER') AS dead_letter_jobs,
    AVG(EXTRACT(EPOCH FROM (completed_at - started_at)) * 1000) FILTER (WHERE completed_at IS NOT NULL) AS avg_execution_time_ms
FROM jobs;

-- View for recent job activity
CREATE OR REPLACE VIEW recent_job_activity AS
SELECT
    j.id,
    j.name,
    j.type,
    j.status,
    j.created_at,
    j.updated_at,
    j.worker_id,
    EXTRACT(EPOCH FROM (COALESCE(j.completed_at, CURRENT_TIMESTAMP) - j.started_at)) * 1000 AS execution_time_ms
FROM jobs j
ORDER BY j.updated_at DESC
LIMIT 100;

-- ============================================================================
-- SAMPLE DATA (Optional - for testing)
-- ============================================================================

-- Insert sample jobs for testing (commented out by default)
-- Uncomment the following lines if you want test data

/*
INSERT INTO jobs (name, type, status, priority, payload) VALUES
    ('Sample Data Processing', 'PROCESS_DATA', 'PENDING', 5, '{"input": "test data", "format": "json"}'),
    ('Welcome Email', 'SEND_EMAIL', 'PENDING', 8, '{"to": "user@example.com", "template": "welcome"}'),
    ('Monthly Report', 'GENERATE_REPORT', 'PENDING', 3, '{"month": "January", "year": 2026}'),
    ('User Sync', 'SYNC_DATA', 'PENDING', 7, '{"source": "ldap", "target": "database"}');
*/

-- ============================================================================
-- GRANTS - Set appropriate permissions
-- ============================================================================

-- Grant all privileges on jobs table
GRANT ALL PRIVILEGES ON TABLE jobs TO jobuser;
GRANT ALL PRIVILEGES ON TABLE job_execution_history TO jobuser;
GRANT SELECT ON job_statistics TO jobuser;
GRANT SELECT ON recent_job_activity TO jobuser;

-- Grant sequence usage
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO jobuser;

-- ============================================================================
-- VERIFICATION
-- ============================================================================

-- Display created objects
DO $$
BEGIN
    RAISE NOTICE 'Database initialization completed successfully!';
    RAISE NOTICE 'Tables created: jobs, job_execution_history';
    RAISE NOTICE 'Views created: job_statistics, recent_job_activity';
    RAISE NOTICE 'Indexes created for optimized queries';
END $$;
