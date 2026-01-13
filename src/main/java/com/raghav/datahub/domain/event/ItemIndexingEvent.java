package com.raghav.datahub.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemIndexingEvent {
    private String podId;
    private String dataItemId;
    private String content;
    private String modelVersion;
}
