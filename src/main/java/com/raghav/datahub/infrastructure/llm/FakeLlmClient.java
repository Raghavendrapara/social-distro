package com.raghav.datahub.infrastructure.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simple fake LLM client used for development/testing.
 * It just echoes part of the prompt and simulates latency.
 */
@Slf4j
@Component
public class FakeLlmClient implements LlmClient {

    private final long simulatedLatencyMs;

    public FakeLlmClient(
            @Value("${datahub.llm.simulated-latency-ms:300}") long simulatedLatencyMs
    ) {
        this.simulatedLatencyMs = simulatedLatencyMs;
    }

    @Override
    public String generateAnswer(String prompt) {
        try {
            Thread.sleep(simulatedLatencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String preview = prompt.length() <= 200 ? prompt : prompt.substring(0, 200) + "...";

        log.debug("FakeLlmClient called with prompt preview: {}", preview);

        return "Fake LLM answer based on prompt: " + preview;
    }
}
