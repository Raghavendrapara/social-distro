package com.raghav.datahub.domain.exception;

/**
 * Thrown when embedding service is unavailable (e.g., model not loaded, Ollama
 * down).
 */
public class EmbeddingUnavailableException extends RuntimeException {

    public EmbeddingUnavailableException(String message) {
        super(message);
    }

    public EmbeddingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
