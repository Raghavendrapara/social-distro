package com.raghav.datahub.domain.model;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single piece of content in a Pod.
 */
@Getter          // <-- This generates getContent(), getId(), getCreatedAt()
@ToString
public class DataItem {

    private final String id;
    private final String content;
    private final Instant createdAt;

    private DataItem(String id, String content, Instant createdAt) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
    }

    /**
     * Factory method to create a new DataItem.
     */
    public static DataItem create(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be null or blank");
        }
        return new DataItem(
                UUID.randomUUID().toString(),
                content,
                Instant.now()
        );
    }
}
