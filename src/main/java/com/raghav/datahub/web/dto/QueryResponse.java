package com.raghav.datahub.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response returned for a query to a Pod.
 */
@Data
@AllArgsConstructor
public class QueryResponse {
    private String answer;
    private String[] usedItemIds;
}
