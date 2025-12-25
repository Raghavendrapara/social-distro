package com.raghav.datahub.web.dto;

public record PodMetadataResponse(
        String id,
        String name,
        String ownerUserId,
        int totalItems
) {}