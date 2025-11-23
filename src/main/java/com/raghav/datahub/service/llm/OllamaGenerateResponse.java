package com.raghav.datahub.service.llm;

import lombok.Data;

@Data
public class OllamaGenerateResponse {
    private String model;
    private String response;
    private boolean done;
}
