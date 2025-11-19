package com.raghav.datahub.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * API response for indexing metrics.
 */
@Data
@AllArgsConstructor
public class IndexingMetricsResponse {

    // Job-level metrics
    private long jobsStarted;
    private long jobsCompleted;
    private long jobsFailed;

    // Chunk-level metrics
    private long chunksProcessed;
    private long chunkFailures;
    private long chunkRetries;

    // Current concurrency
    private int runningJobs;

    // Timing
    private long totalIndexingTimeMs;
    private double averageIndexingTimeMs; // jobsCompleted > 0 ? total / completed : 0
}
