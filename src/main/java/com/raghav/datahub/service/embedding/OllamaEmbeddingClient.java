package com.raghav.datahub.service.embedding;

import com.raghav.datahub.config.LlmProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class OllamaEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final LlmProperties props;

    @Override
    public List<Double> generateEmbedding(String text) {
        var request = new EmbeddingRequest(props.getModel(), text);

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


    private record EmbeddingRequest(String model, String prompt) {}

    private record EmbeddingResponse(List<Double> embedding) {}
}