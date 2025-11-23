package com.raghav.datahub.config;

import com.raghav.datahub.service.llm.FakeLlmClient;
import com.raghav.datahub.service.llm.LlmClient;
import com.raghav.datahub.service.llm.OpenAiLlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
@RequiredArgsConstructor
public class LlmConfig {

    private final LlmProperties props;

    @Bean
    @ConditionalOnProperty(name = "datahub.llm.provider", havingValue = "fake", matchIfMissing = true)
    public LlmClient fakeLlmClient() {
        return new FakeLlmClient(300); // or use props if you want
    }

    @Bean
    @ConditionalOnProperty(name = "datahub.llm.provider", havingValue = "openai")
    public LlmClient openAiLlmClient(WebClient.Builder builder) {
        WebClient webClient = builder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();

        return new OpenAiLlmClient(webClient, props);
    }
}
