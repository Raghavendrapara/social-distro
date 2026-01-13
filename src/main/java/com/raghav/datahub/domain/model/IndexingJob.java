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
        this(
                UUID.randomUUID().toString(),
                podId,
                Instant.now(),
                JobStatus.PENDING,
                null,
                null,
                null);
    }

    /**
     * Full constructor used when loading from persistence.
     * MapStruct will use this constructor automatically.
     */
    public IndexingJob(String jobId,
            String podId,
            Instant createdAt,
            JobStatus status,
            Instant startedAt,
            Instant finishedAt,
            String errorMessage) {
        this.jobId = jobId;
        this.podId = podId;
        this.createdAt = createdAt;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.errorMessage = errorMessage;
    }
}