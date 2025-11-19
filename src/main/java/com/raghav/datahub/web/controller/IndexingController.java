package com.raghav.datahub.web.controller;

import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.service.indexing.IndexingService;
import com.raghav.datahub.web.dto.IndexingJobResponse;
import com.raghav.datahub.web.dto.StartIndexingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/indexing")
@RequiredArgsConstructor
public class IndexingController {

    private final IndexingService indexingService;

    @PostMapping("/pods/{podId}")
    public ResponseEntity<StartIndexingResponse> startIndexing(@PathVariable String podId) {
        IndexingJob job = indexingService.startIndexing(podId);
        return ResponseEntity.accepted().body(new StartIndexingResponse(job.getJobId()));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<IndexingJobResponse> getJob(@PathVariable String jobId) {
        IndexingJob job = indexingService.getJob(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        IndexingJobResponse response = new IndexingJobResponse(
                job.getJobId(),
                job.getPodId(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getErrorMessage()
        );

        return ResponseEntity.ok(response);
    }
}
