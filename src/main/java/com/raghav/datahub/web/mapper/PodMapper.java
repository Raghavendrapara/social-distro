package com.raghav.datahub.web.mapper;

import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.web.dto.PodMetadataResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PodMapper {

    @Mapping(target = "totalItems", expression = "java(pod.getItems().size())")
    PodMetadataResponse toMetadataResponse(Pod pod);
}
