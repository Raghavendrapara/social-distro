package com.raghav.datahub.service.indexing.event;

public record PodIndexingEvent(
        String jobId,
        String podId,
        String traceId
) {}