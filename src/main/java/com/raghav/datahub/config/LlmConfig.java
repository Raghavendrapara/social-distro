package com.raghav.datahub.config;

import com.raghav.datahub.service.embedding.EmbeddingClient;
import com.raghav.datahub.service.embedding.OllamaEmbeddingClient;
import com.raghav.datahub.service.llm.FakeLlmClient;
import com.raghav.datahub.service.llm.LlmClient;
import com.raghav.datahub.service.llm.OllamaLlmClient;
import com.raghav.datahub.service.llm.OpenAiLlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
@RequiredArgsConstructor
public class LlmConfig {

    private final LlmProperties props;

    @Bean
    public RestClient.Builder llmRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(300_000);

        return RestClient.builder().requestFactory(factory);
    }

    @Bean
    @ConditionalOnProperty(name = "datahub.llm.provider", havingValue = "fake", matchIfMissing = true)
    public LlmClient fakeLlmClient() {
        return new FakeLlmClient(300);
    }

    @Bean
    @ConditionalOnProperty(name = "datahub.llm.provider", havingValue = "openai")
    public LlmClient openAiLlmClient(RestClient.Builder llmRestClientBuilder) {
        RestClient restClient = llmRestClientBuilder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
        return new OpenAiLlmClient(restClient, props);
    }

    @Bean
    @ConditionalOnProperty(name = "datahub.llm.provider", havingValue = "ollama")
    public LlmClient ollamaLlmClient(RestClient.Builder llmRestClientBuilder) {
        RestClient restClient = llmRestClientBuilder
                .baseUrl(props.getBaseUrl())
                .build();
        return new OllamaLlmClient(restClient, props);
    }

    @Bean
    public EmbeddingClient embeddingClient(RestClient.Builder llmRestClientBuilder) {
        RestClient restClient = llmRestClientBuilder
                .baseUrl(props.getBaseUrl())
                .build();
        return new OllamaEmbeddingClient(restClient, props);
    }
}