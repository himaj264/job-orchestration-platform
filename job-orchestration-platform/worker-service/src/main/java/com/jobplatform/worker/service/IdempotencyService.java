package com.jobplatform.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Service for managing idempotency using Redis.
 *
 * Prevents duplicate job execution under at-least-once message delivery.
 * Uses Redis SETNX operation for atomic check-and-set.
 *
 * How it works:
 * 1. Before processing a job, check if its ID exists in Redis
 * 2. If not, set the key atomically and proceed with processing
 * 3. If exists, skip processing (duplicate detected)
 * 4. Keys expire after TTL to clean up old entries
 *
 * @author Hima Kammachi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "job:idempotency:";

    @Value("${worker.idempotency.ttl:86400}")
    private long ttlSeconds;

    /**
     * Check if a job has already been processed.
     *
     * Uses Redis SETNX (SET if Not eXists) for atomic check-and-set.
     * Returns true if this is the first time seeing this job ID.
     *
     * @param jobId the job ID to check
     * @return true if job can be processed (not a duplicate)
     */
    public boolean tryAcquire(UUID jobId) {
        String key = buildKey(jobId);

        // SETNX with expiration - atomic operation
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "processing", Duration.ofSeconds(ttlSeconds));

        if (Boolean.TRUE.equals(success)) {
            log.debug("Idempotency key acquired: job-id={}", jobId);
            return true;
        } else {
            log.info("Duplicate job detected, skipping: job-id={}", jobId);
            return false;
        }
    }

    /**
     * Mark a job as completed successfully.
     *
     * Updates the idempotency key value to indicate successful completion.
     * The key will still expire after TTL.
     *
     * @param jobId the job ID
     */
    public void markCompleted(UUID jobId) {
        String key = buildKey(jobId);

        redisTemplate.opsForValue().set(key, "completed", Duration.ofSeconds(ttlSeconds));

        log.debug("Job marked as completed in idempotency store: job-id={}", jobId);
    }

    /**
     * Mark a job as failed.
     *
     * For failed jobs that will be retried, we may want to
     * release the idempotency key to allow reprocessing.
     *
     * @param jobId   the job ID
     * @param release if true, delete the key to allow retry
     */
    public void markFailed(UUID jobId, boolean release) {
        String key = buildKey(jobId);

        if (release) {
            // Delete the key to allow retry
            redisTemplate.delete(key);
            log.debug("Idempotency key released for retry: job-id={}", jobId);
        } else {
            // Keep the key but mark as failed
            redisTemplate.opsForValue().set(key, "failed", Duration.ofSeconds(ttlSeconds));
            log.debug("Job marked as failed in idempotency store: job-id={}", jobId);
        }
    }

    /**
     * Check if a job is already in progress.
     *
     * @param jobId the job ID
     * @return true if the job is already being processed or was processed
     */
    public boolean isProcessed(UUID jobId) {
        String key = buildKey(jobId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Get the status of a job from the idempotency store.
     *
     * @param jobId the job ID
     * @return the status value or null if not found
     */
    public String getStatus(UUID jobId) {
        String key = buildKey(jobId);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Force release an idempotency key.
     *
     * Use with caution - this allows a job to be reprocessed.
     *
     * @param jobId the job ID
     */
    public void release(UUID jobId) {
        String key = buildKey(jobId);
        redisTemplate.delete(key);
        log.info("Idempotency key force released: job-id={}", jobId);
    }

    /**
     * Build the Redis key for a job.
     *
     * @param jobId the job ID
     * @return the Redis key
     */
    private String buildKey(UUID jobId) {
        return IDEMPOTENCY_KEY_PREFIX + jobId.toString();
    }
}
