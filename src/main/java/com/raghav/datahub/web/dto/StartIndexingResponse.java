package com.raghav.datahub.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response after starting an indexing job for a Pod.
 */
@Data
@AllArgsConstructor
public class StartIndexingResponse {
    private String jobId;
}
