package com.raghav.datahub.domain.model;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Getter
@ToString
public class PodIndex {

    private final String podId;
    private final String combinedText;
    private final Instant createdAt;

    public PodIndex(String podId, String combinedText) {
        this.podId = podId;
        this.combinedText = combinedText;
        this.createdAt = Instant.now();
    }
}
