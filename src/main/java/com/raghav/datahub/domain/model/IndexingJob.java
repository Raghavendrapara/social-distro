package com.raghav.datahub.domain.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
public class IndexingJob {

    private final String jobId;
    private final String podId;
    private final Instant createdAt;

    private JobStatus status;
    private Instant startedAt;
    private Instant finishedAt;
    private String errorMessage;

    public IndexingJob(String podId) {
        this.jobId = UUID.randomUUID().toString();
        this.podId = podId;
        this.createdAt = Instant.now();
        this.status = JobStatus.PENDING;
    }
}
