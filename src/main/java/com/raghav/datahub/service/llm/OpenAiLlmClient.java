package com.raghav.datahub.service.llm;

import com.raghav.datahub.config.LlmProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class OpenAiLlmClient implements LlmClient {

    private final WebClient webClient;
    private final LlmProperties props;

    @Override
    public String generateAnswer(String prompt) {
        OpenAiChatRequest request = new OpenAiChatRequest(
                props.getModel(),
                new OpenAiChatRequest.Message[]{
                        new OpenAiChatRequest.Message("system",
                                "You are a helpful assistant that answers based on the provided context."),
                        new OpenAiChatRequest.Message("user", prompt)
                }
        );

        OpenAiChatResponse response = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiChatResponse.class)
                .onErrorResume(ex -> {
                    log.error("Error calling OpenAI LLM", ex);
                    return Mono.just(new OpenAiChatResponse()); // empty response
                })
                .block();

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return "LLM did not return a response.";
        }

        return response.getChoices().getFirst().getMessage().getContent();
    }
}
