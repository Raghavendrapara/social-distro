package com.raghav.datahub.web.dto;

public record QueryResponse(
        String answer,
        String[] usedItemIds
) {}