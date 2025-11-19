package com.raghav.datahub.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response after creating a Pod.
 */
@Data
@AllArgsConstructor
public class CreatePodResponse {
    private String podId;
}
