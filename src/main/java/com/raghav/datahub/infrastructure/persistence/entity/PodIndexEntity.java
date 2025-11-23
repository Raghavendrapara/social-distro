package com.raghav.datahub.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(name = "pod_indexes")
@Getter
@Setter
@ToString
public class PodIndexEntity {

    @Id
    @Column(name = "pod_id", nullable = false, updatable = false)
    private String podId;


    @Column(name = "combined_text", nullable = false, columnDefinition = "TEXT")
    private String combinedText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
