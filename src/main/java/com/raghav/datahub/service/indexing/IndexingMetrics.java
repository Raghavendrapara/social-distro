package com.raghav.datahub.service.indexing;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Simple in-memory metrics for indexing.
 * Thread-safe and lock-free using Atomic types.
 */
@Component
@Getter
public class IndexingMetrics {

    // Job-level metrics
    private final LongAdder jobsStarted = new LongAdder();
    private final LongAdder jobsCompleted = new LongAdder();
    private final LongAdder jobsFailed = new LongAdder();

    // Chunk-level metrics
    private final LongAdder chunksProcessed = new LongAdder();
    private final LongAdder chunkFailures = new LongAdder();
    private final LongAdder chunkRetries = new LongAdder();

    // Current concurrency metrics
    private final AtomicInteger runningJobs = new AtomicInteger(0);

    // Timing metrics (total ms)
    private final AtomicLong totalIndexingTimeMs = new AtomicLong(0L);

    public void incJobsStarted() {
        jobsStarted.increment();
    }

    public void incJobsCompleted() {
        jobsCompleted.increment();
    }

    public void incJobsFailed() {
        jobsFailed.increment();
    }

    public void incChunksProcessed() {
        chunksProcessed.increment();
    }

    public void incChunkFailures() {
        chunkFailures.increment();
    }

    public void incChunkRetries() {
        chunkRetries.increment();
    }

    public void incRunningJobs() {
        runningJobs.incrementAndGet();
    }

    public void decRunningJobs() {
        runningJobs.decrementAndGet();
    }

    public void addIndexingTime(long ms) {
        totalIndexingTimeMs.addAndGet(ms);
    }
}