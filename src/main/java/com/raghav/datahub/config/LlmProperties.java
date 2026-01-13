package com.raghav.datahub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "datahub.llm")
public class LlmProperties {

    /**
     * Provider: "fake", "openai", or "ollama".
     */
    private String provider = "ollama";

    /**
     * Base URL for the LLM provider.
     * For OpenAI: https://api.openai.com
     * For Ollama: http://localhost:11434
     */
    private String baseUrl;

    /**
     * API key for cloud providers like OpenAI.
     * Not used for Ollama.
     */
    private String apiKey;

    /**
     * Model name, e.g.:
     *  - gpt-4o-mini (OpenAI)
     *  - llama3.1 (Ollama)
     */
    private String model;

    private String embeddingModel;
}
