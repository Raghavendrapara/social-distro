package com.raghav.datahub.infrastructure.persistence.mapper;

import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.infrastructure.persistence.entity.IndexingJobEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IndexingJobEntityMapper {

    IndexingJobEntity toEntity(IndexingJob job);

    IndexingJob toDomain(IndexingJobEntity entity);
}