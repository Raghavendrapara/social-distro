package com.raghav.datahub.service.embedding;

import com.raghav.datahub.config.LlmProperties;
import com.raghav.datahub.domain.exception.EmbeddingUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class OllamaEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final LlmProperties props;

    @Override
    @CircuitBreaker(name = "llm", fallbackMethod = "fallbackEmbedding")
    @Cacheable(value = "embedding_cache", key = "#text.hashCode()")
    public List<Double> generateEmbedding(String text) {
        var request = new EmbeddingRequest(props.getEmbeddingModel(), text);

        try {
            var response = restClient.post()
                    .uri("/api/embeddings")
                    .body(request)
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null || response.embedding() == null) {
                log.warn("Ollama returned empty embedding for text: {}",
                        text.substring(0, Math.min(20, text.length())));
                return Collections.emptyList();
            }

            return response.embedding();

        } catch (Exception e) {
            log.error("Failed to fetch embedding from Ollama", e);
            throw e;
        }
    }

    public List<Double> fallbackEmbedding(String text, Throwable t) {
        log.error("Embedding generation failed after circuit breaker. Error: {}", t.getMessage());
        throw new EmbeddingUnavailableException("Embedding service unavailable: " + t.getMessage(), t);
    }

    private record EmbeddingRequest(String model, String prompt) {
    }

    private record EmbeddingResponse(List<Double> embedding) {
    }
}