package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.infrastructure.persistence.entity.VectorChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VectorChunkRepository extends JpaRepository<VectorChunkEntity, String> {

    @Query(value = "SELECT * FROM vector_chunks ORDER BY embedding <-> :embedding LIMIT :limit", nativeQuery = true)
    List<VectorChunkEntity> findSimilar(@Param("embedding") float[] embedding, @Param("limit") int limit);

    List<VectorChunkEntity> findByPodId(String podId);
}
