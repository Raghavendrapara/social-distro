package com.raghav.datahub.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pods")
@Getter
@Setter
@ToString
public class PodEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "owner_user_id", nullable = false, length = 100)
    private String ownerUserId;

    @OneToMany(
            mappedBy = "pod",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @ToString.Exclude
    private List<DataItemEntity> items = new ArrayList<>();
}
