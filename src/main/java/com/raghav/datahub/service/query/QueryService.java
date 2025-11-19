package com.raghav.datahub.service.query;

import com.raghav.datahub.domain.model.DataItem;
import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.domain.model.PodIndex;
import com.raghav.datahub.domain.repository.PodIndexRepository;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.infrastructure.llm.LlmClient;
import com.raghav.datahub.web.dto.QueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final PodRepository podRepository;
    private final PodIndexRepository podIndexRepository;
    private final LlmClient llmClient;

    public QueryResponse queryPod(String podId, String question) {
        Pod pod = podRepository.findById(podId);
        if (pod == null) {
            throw new IllegalArgumentException("Pod not found: " + podId);
        }

        // Try to use indexed content
        PodIndex index = podIndexRepository.findByPodId(podId);
        String context;

        if (index != null) {
            context = index.getCombinedText();
        } else {
            context = pod.getItems()
                    .stream()
                    .map(DataItem::getContent)
                    .collect(Collectors.joining("\n---\n"));
        }

        String prompt = """
                Context:
                %s

                Question:
                %s
                """.formatted(context, question);

        String answer = llmClient.generateAnswer(prompt);

        String[] usedIds = pod.getItems()
                .stream()
                .map(DataItem::getId)
                .toArray(String[]::new);

        return new QueryResponse(answer, usedIds);
    }
}
