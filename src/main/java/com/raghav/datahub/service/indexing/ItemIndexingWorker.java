package com.raghav.datahub.service.indexing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raghav.datahub.domain.event.ItemIndexingEvent;
import com.raghav.datahub.domain.port.VectorStore;
import com.raghav.datahub.infrastructure.persistence.entity.VectorChunkEntity;
import com.raghav.datahub.service.embedding.EmbeddingClient;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemIndexingWorker {

    private static final String DLQ_TOPIC = "item-indexing-events-dlq";

    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "item-indexing-events", groupId = "social-distro-item-workers", concurrency = "5")
    public void onItemEvent(String rawJson, Acknowledgment ack) {
        try {
            ItemIndexingEvent event = objectMapper.readValue(rawJson, ItemIndexingEvent.class);
            processItem(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process item event, sending to DLQ: {}", rawJson, e);
            // Send to Dead Letter Queue
            kafkaTemplate.send(DLQ_TOPIC, rawJson);
            ack.acknowledge();
        }
    }

    private void processItem(ItemIndexingEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            List<Double> embeddingList = embeddingClient.generateEmbedding(event.getContent());
            float[] embeddingArray = toFloatArray(embeddingList);

            VectorChunkEntity chunk = new VectorChunkEntity();
            // Deterministic ID for idempotency (prevents duplicates on Kafka redelivery)
            chunk.setId(event.getPodId() + ":" + event.getDataItemId());
            chunk.setPodId(event.getPodId());
            chunk.setContent(event.getContent());
            chunk.setEmbedding(embeddingArray);
            chunk.setModelVersion(event.getModelVersion());

            vectorStore.saveChunk(chunk);

            sample.stop(meterRegistry.timer("indexing.item.process", "status", "success"));
            log.debug("Indexed item for pod {}", event.getPodId());
        } catch (Exception e) {
            sample.stop(meterRegistry.timer("indexing.item.process", "status", "error"));
            log.error("Error generating embedding for item in pod {}", event.getPodId(), e);
            throw e;
        }
    }

    private float[] toFloatArray(List<Double> list) {
        if (list == null || list.isEmpty())
            return new float[0];
        return IntStream.range(0, list.size())
                .mapToDouble(i -> list.get(i) != null ? list.get(i) : 0.0)
                .collect(() -> new float[list.size()],
                        (arr, val) -> arr[0] = (float) val,
                        (a, b) -> {
                        });
    }
}
