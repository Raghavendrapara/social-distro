package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.domain.repository.IndexingJobRepository;
import com.raghav.datahub.infrastructure.persistence.entity.IndexingJobEntity;
import com.raghav.datahub.infrastructure.persistence.mapper.IndexingJobEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
@RequiredArgsConstructor
public class JpaIndexingJobRepositoryAdapter implements IndexingJobRepository {

    private final JpaIndexingJobSpringRepository springRepository;
    private final IndexingJobEntityMapper mapper;

    @Override
    public IndexingJob save(IndexingJob job) {
        IndexingJobEntity entity = mapper.toEntity(job);

        IndexingJobEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public IndexingJob findById(String jobId) {
        return springRepository.findById(jobId)
                .map(mapper::toDomain)
                .orElse(null);
    }
}