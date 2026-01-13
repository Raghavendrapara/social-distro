package com.raghav.datahub.service.indexing;

import com.raghav.datahub.domain.model.*;
import com.raghav.datahub.domain.repository.IndexingJobRepository;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.infrastructure.persistence.entity.VectorChunkEntity;
import com.raghav.datahub.infrastructure.persistence.repository.JpaVectorChunkRepository;
import com.raghav.datahub.service.embedding.EmbeddingClient;
import com.raghav.datahub.service.indexing.event.PodIndexingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingWorker {

    private final PodRepository podRepository;
    private final IndexingJobRepository jobRepository;
    private final EmbeddingClient embeddingClient;
    private final JpaVectorChunkRepository vectorChunkRepository;
    private final IndexingMetrics metrics;

    @KafkaListener(topics = "pod-indexing-jobs", groupId = "social-distro-workers", concurrency = "3")
    public void processJob(PodIndexingEvent event) {
        log.info("Worker received job: {}", event);

        IndexingJob job = jobRepository.findById(event.jobId());
        if (job == null) return;

        updateJobStatus(job, JobStatus.RUNNING);

        try {
            AtomicInteger count = new AtomicInteger();

            podRepository.streamItems(event.podId(), item -> {
                processSingleItem(event.podId(), item);
                count.incrementAndGet();
            });

            log.info("Processed {} items for pod {}", count.get(), event.podId());
            updateJobStatus(job, JobStatus.COMPLETED);
            metrics.incJobsCompleted();

        } catch (Exception e) {
            log.error("Job failed", e);
            job.setErrorMessage(e.getMessage());
            updateJobStatus(job, JobStatus.FAILED);
            metrics.incJobsFailed();
            throw new RuntimeException("Triggering Kafka Retry", e);
        }
    }

    private void processSingleItem(String podId, DataItem item) {
        List<Double> embedding = embeddingClient.generateEmbedding(item.getContent());
        if (embedding.isEmpty()) return;

        VectorChunkEntity entity = new VectorChunkEntity();
        entity.setPodId(podId);
        entity.setContent(item.getContent());

        float[] floatArray = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            floatArray[i] = embedding.get(i).floatValue();
        }
        entity.setEmbedding(floatArray);

        vectorChunkRepository.save(entity);
        metrics.incChunksProcessed();
    }

    private void updateJobStatus(IndexingJob job, JobStatus status) {
        job.setStatus(status);
        if (status == JobStatus.RUNNING) job.setStartedAt(Instant.now());
        if (status == JobStatus.COMPLETED || status == JobStatus.FAILED) job.setFinishedAt(Instant.now());
        jobRepository.save(job);
    }
}