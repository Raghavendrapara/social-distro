package com.raghav.datahub.service.llm;

import com.raghav.datahub.config.LlmProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class OpenAiLlmClient implements LlmClient {

    private final RestClient restClient;
    private final LlmProperties props;

    @Override
    public String generateAnswer(String prompt) {
        OpenAiChatRequest request = new OpenAiChatRequest(
                props.getModel(),
                new OpenAiChatRequest.Message[]{
                        new OpenAiChatRequest.Message("system",
                                "You are a helpful assistant."),
                        new OpenAiChatRequest.Message("user", prompt)
                }
        );

        try {
            OpenAiChatResponse response = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(OpenAiChatResponse.class);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                return "LLM did not return a response.";
            }
            return response.getChoices().getFirst().getMessage().getContent();

        } catch (Exception e) {
            log.error("Error calling OpenAI LLM", e);
            return "Error calling OpenAI: " + e.getMessage();
        }
    }
}