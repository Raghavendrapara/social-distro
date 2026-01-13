package com.raghav.datahub.infrastructure.persistence.mapper;

import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.infrastructure.persistence.entity.IndexingJobEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IndexingJobEntityMapper {

    IndexingJobEntity toEntity(IndexingJob job);

    /**
     * ObjectFactory to explicitly construct IndexingJob using the full constructor.
     * This avoids needing @Default annotation in the domain model.
     */
    @ObjectFactory
    default IndexingJob createIndexingJob(IndexingJobEntity entity) {
        return new IndexingJob(
                entity.getJobId(),
                entity.getPodId(),
                entity.getCreatedAt(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getErrorMessage());
    }

    IndexingJob toDomain(IndexingJobEntity entity);
}