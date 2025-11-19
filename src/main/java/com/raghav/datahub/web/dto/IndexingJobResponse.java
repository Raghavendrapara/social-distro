package com.raghav.datahub.web.dto;

import com.raghav.datahub.domain.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * Response representing the status of an indexing job.
 */
@Data
@AllArgsConstructor
public class IndexingJobResponse {

    private String jobId;
    private String podId;
    private JobStatus status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private String errorMessage;
}
