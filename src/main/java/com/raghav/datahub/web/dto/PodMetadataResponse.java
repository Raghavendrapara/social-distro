package com.raghav.datahub.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PodMetadataResponse {

    private String id;
    private String name;
    private String ownerUserId;
    private int totalItems;
}
