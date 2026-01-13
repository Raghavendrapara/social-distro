package com.raghav.datahub.domain.model;

import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ToString
public class Pod {

    private final String id;
    private String name;
    private String ownerUserId;
    private final List<DataItem> items;

    public Pod(String name, String ownerUserId) {
        this(UUID.randomUUID().toString(), name, ownerUserId, new ArrayList<>());
    }

    /**
     * Full constructor used when loading from persistence.
     * MapStruct will use this constructor automatically (most parameters).
     */
    public Pod(String id, String name, String ownerUserId, List<DataItem> items) {
        this.id = id;
        this.name = name;
        this.ownerUserId = ownerUserId;
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    /**
     * Returns an unmodifiable view of items to prevent external modification.
     */
    public List<DataItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Thread-safe method to add an item.
     */
    public synchronized void addItem(DataItem item) {
        this.items.add(item);
    }
}