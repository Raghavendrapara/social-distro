package com.raghav.datahub.service.indexing;

import com.raghav.datahub.domain.model.DataItem;
import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.domain.model.JobStatus;
import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.domain.repository.IndexingJobRepository;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.service.embedding.EmbeddingClient;
import com.raghav.datahub.service.indexing.event.PodIndexingEvent;
import com.raghav.datahub.infrastructure.persistence.entity.VectorChunkEntity;
import com.raghav.datahub.infrastructure.persistence.repository.JpaVectorChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Kafka Listener.
     * We accept the event, but delegate the *risky* logic to a method marked with @Retryable.
     * This separates the "Transportation Layer" (Kafka) from the "Business Logic" (Processing).
     */
    @KafkaListener(
            topics = "pod-indexing-jobs",
            groupId = "social-distro-workers",
            concurrency = "3"
    )
    public void onIndexingEvent(PodIndexingEvent event, Acknowledgment ack) {
        log.info("Received Indexing Job: {}", event.jobId());
        try {
            processWithRetry(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Job {} permanently failed. Moving to DLQ logic.", event.jobId());
            ack.acknowledge();
        }
    }

    /**
     * The "Risky" Method.
     * Retries automatically on RuntimeExceptions (like DB timeouts, LLM 503s).
     * Backoff: 1000ms * 2^(attempt) -> 1s, 2s, 4s.
     */
    @Retryable(
            retryFor = { RuntimeException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
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
        List<Double> embedding = embeddingClient.generateEmbedding(item.getContent());

        VectorChunkEntity chunk = new VectorChunkEntity();
        chunk.setId(UUID.randomUUID().toString());
        chunk.setPodId(podId);
        chunk.setContent(item.getContent());
        chunk.setEmbedding(embedding);

        vectorChunkRepository.save(chunk);
    }

    /**
     * Fallback method called when all 3 retries fail.
     */
    @Recover
    public void recover(RuntimeException e, PodIndexingEvent event) {
        log.error("CRITICAL FAILURE: Job {} failed after retries. Error: {}", event.jobId(), e.getMessage());

        IndexingJob job = jobRepository.findById(event.jobId());
        if (job != null) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(Instant.now());
            jobRepository.save(job);
        }
    }
}