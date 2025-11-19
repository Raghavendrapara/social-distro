package com.raghav.datahub.domain.model;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Core domain object representing a "Pod".
 * Created only via the static factory method "create".
 */
@Getter
@ToString
public class Pod {

    private final String id;
    private final String name;
    private final String ownerUserId;

    private final List<DataItem> items = new ArrayList<>();

    /**
     * Private constructor — only static factory can create Pods.
     */
    private Pod(String id, String name, String ownerUserId) {
        this.id = id;
        this.name = name;
        this.ownerUserId = ownerUserId;
    }

    /**
     * Static factory method.
     * This MUST exist because PodService calls Pod.create(name, ownerUserId).
     */
    public static Pod create(String name, String ownerUserId) {
        return new Pod(UUID.randomUUID().toString(), name, ownerUserId);
    }

    /**
     * Add a data item to this Pod.
     */
    public void addItem(DataItem item) {
        this.items.add(item);
    }

    /**
     * Items exposed as unmodifiable list.
     */
    public List<DataItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
