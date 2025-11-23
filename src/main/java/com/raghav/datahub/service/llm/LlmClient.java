package com.raghav.datahub.service.llm;

/**
 * Abstraction for an LLM client.
 *
 * Implementations can be:
 *  - FakeLlmClient (for local/dev)
 *  - OpenAiLlmClient
 *  - LocalModelLlmClient
 */
public interface LlmClient {

    /**
     * Generate an answer given a prepared prompt.
     */
    String generateAnswer(String prompt);
}
