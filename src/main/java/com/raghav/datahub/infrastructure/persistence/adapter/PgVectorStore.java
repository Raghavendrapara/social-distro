package com.raghav.datahub.infrastructure.persistence.adapter;

import com.raghav.datahub.domain.port.VectorStore;
import com.raghav.datahub.infrastructure.persistence.entity.VectorChunkEntity;
import com.raghav.datahub.infrastructure.persistence.repository.VectorChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PgVectorStore implements VectorStore {

    private final VectorChunkRepository vectorChunkRepository;

    @Override
    public void saveChunk(VectorChunkEntity chunk) {
        vectorChunkRepository.save(chunk);
    }

    @Override
    public List<VectorChunkEntity> findSimilar(float[] embedding, int limit) {
        return vectorChunkRepository.findSimilar(embedding, limit);
    }

    @Override
    public List<VectorChunkEntity> findSimilarByPodId(String podId, float[] embedding, int limit) {
        return vectorChunkRepository.findSimilarByPodId(podId, embedding, limit);
    }

    @Override
    public List<VectorChunkEntity> findByPodId(String podId) {
        return vectorChunkRepository.findByPodId(podId);
    }
}
