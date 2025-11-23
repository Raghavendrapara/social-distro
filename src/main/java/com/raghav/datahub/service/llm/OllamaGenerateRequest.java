package com.raghav.datahub.service.llm;

import lombok.Data;

@Data
public class OllamaGenerateRequest {
    private String model;
    private String prompt;
    private boolean stream;
}
