package com.raghav.datahub.service.indexing;

import com.raghav.datahub.domain.model.DataItem;
import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.domain.model.JobStatus;
import com.raghav.datahub.domain.repository.IndexingJobRepository;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.service.embedding.EmbeddingClient;
import com.raghav.datahub.service.indexing.event.PodIndexingEvent;
import com.raghav.datahub.infrastructure.persistence.entity.VectorChunkEntity;
import com.raghav.datahub.infrastructure.persistence.repository.JpaVectorChunkRepository;
import io.github.resilience4j.retry.annotation.Retry;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingWorker {

    private final PodRepository podRepository;
    private final IndexingJobRepository jobRepository;
    private final EmbeddingClient embeddingClient;
    private final JpaVectorChunkRepository vectorChunkRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        log.info("=================================================");
        log.info("✅ INDEXING WORKER ALIVE (Group: social-distro-workers-v2)");
        log.info("=================================================");
    }


    @KafkaListener(
            topics = "pod-indexing-jobs",
            groupId = "social-distro-workers-v2",
            concurrency = "3"
    )
    public void onIndexingEvent(String rawJson, Acknowledgment ack) {
        log.info("Received Raw Kafka Message: {}", rawJson);
        PodIndexingEvent event;
        try {
            event = objectMapper.readValue(rawJson, PodIndexingEvent.class);
        } catch (Exception e) {
            log.error("Failed to parse Kafka message: {}", rawJson, e);
            ack.acknowledge();
            return;
        }

        log.info("Successfully parsed Job ID: {}", event.jobId());
        try {
            processWithRetry(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Job {} permanently failed after retries.", event.jobId());
            handlePermanentFailure(event, e);
            ack.acknowledge();
        }
    }

    @Retry(name = "indexingRetry", fallbackMethod = "recover")
    public void processWithRetry(PodIndexingEvent event) {
        IndexingJob job = jobRepository.findById(event.jobId());
        if (job == null) {
            log.error("Job not found: {}", event.jobId());
            return;
        }

        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        AtomicInteger count = new AtomicInteger();

        podRepository.streamItems(event.podId(), item -> {
            processItem(event.podId(), item);
            count.incrementAndGet();
        });

        job.setStatus(JobStatus.COMPLETED);
        job.setFinishedAt(Instant.now());
        jobRepository.save(job);

        log.info("Job {} completed. Processed {} items.", event.jobId(), count.get());
    }

    private void processItem(String podId, DataItem item) {
        List<Double> embeddingList = embeddingClient.generateEmbedding(item.getContent());

        float[] embeddingArray = toFloatArray(embeddingList);

        VectorChunkEntity chunk = new VectorChunkEntity();
        chunk.setId(UUID.randomUUID().toString());
        chunk.setPodId(podId);
        chunk.setContent(item.getContent());
        chunk.setEmbedding(embeddingArray);

        vectorChunkRepository.save(chunk);
    }

    public void recover(PodIndexingEvent event, Exception e) {
        throw new RuntimeException("Resilience4j Retry exhausted", e);
    }

    private void handlePermanentFailure(PodIndexingEvent event, Exception e) {
        IndexingJob job = jobRepository.findById(event.jobId());
        if (job != null) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(Instant.now());
            jobRepository.save(job);
        }
    }

    private float[] toFloatArray(List<Double> list) {
        if (list == null) return new float[0];
        float[] floatArray = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Double val = list.get(i);
            floatArray[i] = (val != null) ? val.floatValue() : 0.0f;
        }
        return floatArray;
    }
}