package com.raghav.datahub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "datahub.llm")
public class LlmProperties {

    /**
     * Provider: "fake" or "openai".
     */
    private String provider = "fake";

    /**
     * Base URL for OpenAI-compatible API.
     * Example: https://api.openai.com
     */
    private String baseUrl;

    /**
     * API key for the LLM provider.
     */
    private String apiKey;

    /**
     * Model name, e.g. "gpt-4o-mini" or "gpt-4.1".
     */
    private String model;
}
