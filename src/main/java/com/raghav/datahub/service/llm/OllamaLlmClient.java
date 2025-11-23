package com.raghav.datahub.service.llm;

import com.raghav.datahub.config.LlmProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class OllamaLlmClient implements LlmClient {

    private final WebClient webClient;
    private final LlmProperties props;

    @Override
    public String generateAnswer(String prompt) {
        OllamaGenerateRequest request = new OllamaGenerateRequest();
        request.setModel(props.getModel());
        request.setPrompt(prompt);
        request.setStream(false);

        OllamaGenerateResponse response = webClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaGenerateResponse.class)
                .onErrorResume(ex -> {
                    log.error("Error calling Ollama LLM", ex);
                    return Mono.just(new OllamaGenerateResponse()); // empty response
                })
                .block(); // we'll make this reactive later in Step C

        if (response == null || response.getResponse() == null) {
            return "LLM did not return a response.";
        }

        return response.getResponse().trim();
    }
}
