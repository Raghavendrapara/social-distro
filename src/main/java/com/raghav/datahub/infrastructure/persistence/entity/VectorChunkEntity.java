package com.raghav.datahub.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "vector_chunks")
@Getter
@Setter
public class VectorChunkEntity {

    @Id
    private String id;

    @Column(name = "pod_id", nullable = false)
    private String podId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "embedding")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 3072)
    private float[] embedding;

    public VectorChunkEntity() {
        this.id = UUID.randomUUID().toString();
    }
}