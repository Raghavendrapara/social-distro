package com.raghav.datahub.web.controller;

import com.raghav.datahub.service.indexing.IndexingMetrics;
import com.raghav.datahub.web.dto.IndexingMetricsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Simple internal metrics endpoint.
 * In real production you might protect this with auth or move to Actuator.
 */
@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final IndexingMetrics indexingMetrics;

    @GetMapping("/indexing")
    public ResponseEntity<IndexingMetricsResponse> getIndexingMetrics() {
        long jobsStarted = indexingMetrics.getJobsStarted().sum();
        long jobsCompleted = indexingMetrics.getJobsCompleted().sum();
        long jobsFailed = indexingMetrics.getJobsFailed().sum();

        long chunksProcessed = indexingMetrics.getChunksProcessed().sum();
        long chunkFailures = indexingMetrics.getChunkFailures().sum();
        long chunkRetries = indexingMetrics.getChunkRetries().sum();

        int runningJobs = indexingMetrics.getRunningJobs().get();

        long totalTimeMs = indexingMetrics.getTotalIndexingTimeMs().get();
        double avgTimeMs = jobsCompleted > 0
                ? (double) totalTimeMs / jobsCompleted
                : 0.0;

        IndexingMetricsResponse response = new IndexingMetricsResponse(
                jobsStarted,
                jobsCompleted,
                jobsFailed,
                chunksProcessed,
                chunkFailures,
                chunkRetries,
                runningJobs,
                totalTimeMs,
                avgTimeMs
        );

        return ResponseEntity.ok(response);
    }
}
