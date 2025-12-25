package com.raghav.datahub.web.dto;

public record IndexingMetricsResponse(
        long jobsStarted,
        long jobsCompleted,
        long jobsFailed,
        long chunksProcessed,
        long chunkFailures,
        long chunkRetries,
        int runningJobs,
        long totalIndexingTimeMs,
        double averageIndexingTimeMs
) {}