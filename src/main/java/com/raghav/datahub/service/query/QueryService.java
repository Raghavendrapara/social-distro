package com.raghav.datahub.service.query;

import com.raghav.datahub.domain.exception.PodNotFoundException;
import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.domain.model.PodIndex;
import com.raghav.datahub.domain.port.VectorStore;
import com.raghav.datahub.domain.repository.PodIndexRepository;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.infrastructure.persistence.entity.VectorChunkEntity;
import com.raghav.datahub.service.embedding.EmbeddingClient;
import com.raghav.datahub.service.llm.LlmClient;
import com.raghav.datahub.web.dto.QueryRequest;
import com.raghav.datahub.web.dto.QueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {

    private static final int TOP_K_CHUNKS = 5;

    private final PodRepository podRepository;
    private final PodIndexRepository podIndexRepository;
    private final VectorStore vectorStore;
    private final EmbeddingClient embeddingClient;
    private final LlmClient llmClient;

    public QueryResponse queryPod(String podId, QueryRequest request) {
        Pod pod = podRepository.findById(podId);
        if (pod == null) {
            throw new PodNotFoundException(podId);
        }

        String context;
        String[] usedItemIds;

        try {
            // RAG path: embedding + vector search (scoped to this pod)
            List<Double> questionEmbedding = embeddingClient.generateEmbedding(request.question());
            float[] embeddingArray = toFloatArray(questionEmbedding);
            List<VectorChunkEntity> similarChunks = vectorStore.findSimilarByPodId(podId, embeddingArray, TOP_K_CHUNKS);
            context = buildContextFromChunks(similarChunks);
            usedItemIds = similarChunks.stream().map(VectorChunkEntity::getId).toArray(String[]::new);
        } catch (Exception e) {
            log.warn("RAG search failed, falling back to PodIndex. Error: {}", e.getMessage());
            // Fallback: use aggregated PodIndex text
            PodIndex podIndex = podIndexRepository.findByPodId(podId);
            context = podIndex != null ? podIndex.getCombinedText() : "";
            usedItemIds = new String[] { "fallback:pod-index" };
        }

        String prompt = buildPrompt(context, request.question());
        String answer = llmClient.generateAnswer(prompt);

        return new QueryResponse(answer, usedItemIds);
    }

    private String buildContextFromChunks(List<VectorChunkEntity> chunks) {
        StringBuilder sb = new StringBuilder();
        for (VectorChunkEntity chunk : chunks) {
            sb.append(chunk.getContent()).append("\n");
        }
        return sb.toString();
    }

    private String buildPrompt(String context, String question) {
        return """
                Context:
                %s

                Question:
                %s
                """.formatted(context, question);
    }

    private float[] toFloatArray(List<Double> list) {
        if (list == null || list.isEmpty())
            return new float[0];
        float[] floatArray = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Double val = list.get(i);
            floatArray[i] = (val != null) ? val.floatValue() : 0.0f;
        }
        return floatArray;
    }
}
