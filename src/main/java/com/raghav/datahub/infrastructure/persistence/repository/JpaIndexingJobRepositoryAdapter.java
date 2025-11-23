package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.domain.model.JobStatus;
import com.raghav.datahub.domain.repository.IndexingJobRepository;
import com.raghav.datahub.infrastructure.persistence.entity.IndexingJobEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
@RequiredArgsConstructor
public class JpaIndexingJobRepositoryAdapter implements IndexingJobRepository {

    private final JpaIndexingJobSpringRepository springRepository;

    @Override
    public IndexingJob save(IndexingJob job) {
        IndexingJobEntity entity = toEntity(job);
        IndexingJobEntity saved = springRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public IndexingJob findById(String jobId) {
        return springRepository.findById(jobId)
                .map(this::toDomain)
                .orElse(null);
    }

    private IndexingJobEntity toEntity(IndexingJob job) {
        IndexingJobEntity e = new IndexingJobEntity();
        e.setJobId(job.getJobId());
        e.setPodId(job.getPodId());
        e.setCreatedAt(job.getCreatedAt());
        e.setStatus(job.getStatus() != null ? job.getStatus() : JobStatus.PENDING);
        e.setStartedAt(job.getStartedAt());
        e.setFinishedAt(job.getFinishedAt());
        e.setErrorMessage(job.getErrorMessage());
        return e;
    }

    private IndexingJob toDomain(IndexingJobEntity e) {
        return new IndexingJob(
                e.getJobId(),
                e.getPodId(),
                e.getCreatedAt(),
                e.getStatus(),
                e.getStartedAt(),
                e.getFinishedAt(),
                e.getErrorMessage()
        );
    }
}
