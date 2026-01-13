package com.raghav.datahub.service.llm;

import com.raghav.datahub.config.LlmProperties;
import com.raghav.datahub.web.dto.AddDataRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class OllamaLlmClient implements LlmClient {

    private final RestClient restClient;
    private final LlmProperties props;

    @Override
    public String generateAnswer(String prompt) {
        OllamaGenerateRequest request = new OllamaGenerateRequest();
        request.setModel(props.getModel());
        request.setPrompt(prompt);
        request.setStream(false);

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
}