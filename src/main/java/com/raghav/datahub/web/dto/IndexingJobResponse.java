package com.raghav.datahub.web.dto;

import com.raghav.datahub.domain.model.JobStatus;
import java.time.Instant;

public record IndexingJobResponse(
        String jobId,
        String podId,
        JobStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage
) {}