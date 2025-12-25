package com.raghav.datahub.service.query;

import com.raghav.datahub.domain.model.DataItem;
import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.domain.model.PodIndex;
import com.raghav.datahub.domain.repository.PodIndexRepository;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.service.llm.LlmClient;
import com.raghav.datahub.web.dto.QueryRequest;
import com.raghav.datahub.web.dto.QueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final PodRepository podRepository;
    private final PodIndexRepository podIndexRepository;
    private final LlmClient llmClient;

    public QueryResponse queryPod(String podId, QueryRequest request) {
        Pod pod = podRepository.findById(podId);
        if (pod == null) {
            throw new IllegalArgumentException("Pod not found: " + podId);
        }

        String context = buildContext(pod);
        String prompt = buildPrompt(context, request.question());

        String answer = llmClient.generateAnswer(prompt);

        String[] usedItemIds = pod.getItems().stream()
                .map(DataItem::getId)
                .toArray(String[]::new);

        return new QueryResponse(answer, usedItemIds);
    }

    private String buildContext(Pod pod) {
        PodIndex index = podIndexRepository.findByPodId(pod.getId());
        if (index != null) {
            return index.getCombinedText();
        }

        StringBuilder sb = new StringBuilder();
        for (DataItem item : pod.getItems()) {
            sb.append(item.getContent()).append("\n");
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
}
