package com.raghav.datahub.infrastructure.persistence.mapper;

import com.raghav.datahub.domain.model.DataItem;
import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.infrastructure.persistence.entity.DataItemEntity;
import com.raghav.datahub.infrastructure.persistence.entity.PodEntity;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * ObjectFactory to explicitly construct Pod using the full constructor.
     * This avoids needing @Default annotation in the domain model.
     */
    @ObjectFactory
    default Pod createPod(PodEntity entity) {
        List<DataItem> items = entity.getItems() != null
                ? entity.getItems().stream().map(this::toItemDomain).collect(Collectors.toList())
                : new ArrayList<>();
        return new Pod(entity.getId(), entity.getName(), entity.getOwnerUserId(), items);
    }

    // MapStruct will use createPod factory, items already handled there
    @Mapping(target = "items", ignore = true)
    Pod toDomain(PodEntity entity);

    DataItem toItemDomain(DataItemEntity entity);
}