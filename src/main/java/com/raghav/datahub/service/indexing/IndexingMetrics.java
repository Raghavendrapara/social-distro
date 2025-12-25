package com.raghav.datahub.service.indexing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modern Observability implementation.
 * Delegates to Micrometer MeterRegistry so metrics appear in Actuator endpoints.
 */
@Component
public class IndexingMetrics {

    private final Counter jobsStarted;
    private final Counter jobsCompleted;
    private final Counter jobsFailed;

    private final Counter chunksProcessed;
    private final Counter chunkFailures;
    private final Counter chunkRetries;

    private final AtomicInteger runningJobsGauge = new AtomicInteger(0);
    private final Timer indexingTimer;

    public IndexingMetrics(MeterRegistry registry) {
        this.jobsStarted = registry.counter("datahub.indexing.jobs.started");
        this.jobsCompleted = registry.counter("datahub.indexing.jobs.completed");
        this.jobsFailed = registry.counter("datahub.indexing.jobs.failed");

        this.chunksProcessed = registry.counter("datahub.indexing.chunks.processed");
        this.chunkFailures = registry.counter("datahub.indexing.chunks.failed");
        this.chunkRetries = registry.counter("datahub.indexing.chunks.retries");

        Gauge.builder("datahub.indexing.jobs.running", runningJobsGauge, AtomicInteger::get)
                .description("Number of currently running indexing jobs")
                .register(registry);

        this.indexingTimer = Timer.builder("datahub.indexing.time")
                .description("Time taken to index a pod")
                .register(registry);
    }


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
        runningJobsGauge.incrementAndGet();
    }

    public void decRunningJobs() {
        runningJobsGauge.decrementAndGet();
    }

    public void addIndexingTime(long ms) {
        indexingTimer.record(ms, TimeUnit.MILLISECONDS);
    }
}