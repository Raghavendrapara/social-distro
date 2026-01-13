package com.raghav.datahub.service.llm;

import com.raghav.datahub.config.LlmProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
@Service
public class OllamaLlmClient implements LlmClient {

    private final RestClient restClient;
    private final LlmProperties props;

    @Override
    @CircuitBreaker(name = "llm", fallbackMethod = "generateFallback")
    @Cacheable(value = "llm_responses", key = "#prompt")
    public String generateAnswer(String prompt) {
        OllamaGenerateRequest request = new OllamaGenerateRequest();
        request.setModel(props.getModel());
        request.setPrompt(prompt);
        request.setStream(false);

        log.debug("Sending request to Ollama: {}", props.getModel());

        OllamaGenerateResponse response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .body(OllamaGenerateResponse.class);

        if (response == null || response.getResponse() == null) {
            return "LLM did not return a response.";
        }

        return response.getResponse().trim();
    }

    public String generateFallback(String prompt, Throwable t) {
        log.warn("Ollama is down or failing. Returning fallback response. Error: {}", t.getMessage());
        return "Comparison temporarily unavailable due to high load. Please try again later.";
    }
}