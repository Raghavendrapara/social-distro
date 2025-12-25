package com.raghav.datahub.service.embedding;

import java.util.List;

public interface EmbeddingClient {
    List<Double> generateEmbedding(String text);
}