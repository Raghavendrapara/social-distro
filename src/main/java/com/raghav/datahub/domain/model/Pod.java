package com.raghav.datahub.domain.model;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@ToString
public class Pod {

    private final String id;
    private String name;
    private String ownerUserId;
    private final List<DataItem> items;

    /**
     * Constructor used when creating a brand new pod in the application.
     */
    public Pod(String name, String ownerUserId) {
        this(UUID.randomUUID().toString(), name, ownerUserId, new ArrayList<>());
    }

    /**
     * Full constructor used when loading from persistence.
     */
    public Pod(String id, String name, String ownerUserId, List<DataItem> items) {
        this.id = id;
        this.name = name;
        this.ownerUserId = ownerUserId;
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
    }

    public void addItem(DataItem item) {
        this.items.add(item);
    }
}
