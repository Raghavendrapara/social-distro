package com.raghav.datahub.domain.model;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
public class DataItem {

    private final String id;
    private final String content;
    private final Instant createdAt;

    public DataItem(String content) {
        this(UUID.randomUUID().toString(), content, Instant.now());
    }

    public DataItem(String id, String content, Instant createdAt) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
    }
}
