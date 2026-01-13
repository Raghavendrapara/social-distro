package com.raghav.datahub.infrastructure.persistence.mapper;

import com.raghav.datahub.domain.model.DataItem;
import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.infrastructure.persistence.entity.DataItemEntity;
import com.raghav.datahub.infrastructure.persistence.entity.PodEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PodEntityMapper {


    @Mapping(target = "items", ignore = true)
    PodEntity toEntity(Pod pod);

    @Mapping(target = "pod", ignore = true)
    DataItemEntity toItemEntity(DataItem item);

    @AfterMapping
    default void linkItems(@MappingTarget PodEntity podEntity, Pod pod) {
        if (pod.getItems() != null) {
            List<DataItemEntity> entities = pod.getItems().stream()
                    .map(this::toItemEntity)
                    .peek(itemEntity -> itemEntity.setPod(podEntity))
                    .toList();
            podEntity.setItems(entities);
        }
    }

    Pod toDomain(PodEntity entity);

    DataItem toItemDomain(DataItemEntity entity);
}