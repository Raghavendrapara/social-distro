package com.raghav.datahub.service.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class FakeLlmClient implements LlmClient {

    private final long simulatedLatencyMs;

    public FakeLlmClient(@Value("${datahub.llm.simulated-latency-ms:300}") long simulatedLatencyMs) {
        this.simulatedLatencyMs = simulatedLatencyMs;
    }

    @Override
    public String generateAnswer(String prompt) {
        try {
            Thread.sleep(simulatedLatencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("FakeLlmClient called with prompt:\n{}", prompt);
        return "Fake LLM answer based on prompt:\n\n" + prompt;
    }
}
