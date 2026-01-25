package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.infrastructure.persistence.entity.VectorChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VectorChunkRepository extends JpaRepository<VectorChunkEntity, String> {

    /**
     * Find similar vectors across ALL pods (global search).
     * Consider using findSimilarByPodId for pod-scoped queries.
     */
    @Query(value = "SELECT * FROM vector_chunks ORDER BY embedding <-> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<VectorChunkEntity> findSimilar(@Param("embedding") float[] embedding, @Param("limit") int limit);

    /**
     * Find similar vectors within a specific pod (recommended for user queries).
     * Uses HNSW index for fast approximate nearest neighbor search.
     */
    @Query(value = "SELECT * FROM vector_chunks WHERE pod_id = :podId ORDER BY embedding <-> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<VectorChunkEntity> findSimilarByPodId(@Param("podId") String podId, @Param("embedding") float[] embedding,
            @Param("limit") int limit);

    List<VectorChunkEntity> findByPodId(String podId);
}
