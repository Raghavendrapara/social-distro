package com.raghav.datahub.domain.exception;

public class IndexingFailedException extends RuntimeException {
    public IndexingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
