package com.raghav.datahub.domain.port;

import com.raghav.datahub.infrastructure.persistence.entity.VectorChunkEntity;

import java.util.List;

public interface VectorStore {
    void saveChunk(VectorChunkEntity chunk);

    List<VectorChunkEntity> findSimilar(float[] embedding, int limit);

    List<VectorChunkEntity> findByPodId(String podId);
}
