package com.raghav.datahub.domain.exception;

public class PodNotFoundException extends RuntimeException {
    public PodNotFoundException(String podId) {
        super("Pod not found: " + podId);
    }
}
