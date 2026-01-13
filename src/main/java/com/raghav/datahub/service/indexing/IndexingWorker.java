package com.raghav.datahub.service.indexing;

import com.raghav.datahub.config.LlmProperties;
import com.raghav.datahub.domain.event.ItemIndexingEvent;
import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.domain.model.JobStatus;
import com.raghav.datahub.domain.model.PodIndex;
import com.raghav.datahub.domain.repository.IndexingJobRepository;
import com.raghav.datahub.domain.repository.PodIndexRepository;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.service.indexing.event.PodIndexingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingWorker {

    private final PodRepository podRepository;
    private final IndexingJobRepository jobRepository;
    private final PodIndexRepository podIndexRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final LlmProperties llmProperties;

    @KafkaListener(topics = "pod-indexing-jobs", groupId = "social-distro-workers-v3", concurrency = "3", containerFactory = "kafkaListenerContainerFactory")
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
    @Transactional
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
        StringBuilder combinedText = new StringBuilder();

        // Fan-out: Iterate items and send events
        podRepository.streamItems(event.podId(), item -> {
            // Send to Kafka for async processing
            ItemIndexingEvent itemEvent = new ItemIndexingEvent(
                    event.podId(),
                    item.getId(),
                    item.getContent(),
                    llmProperties.getEmbeddingModel());
            // Use item.getId() as key for partitioning
            kafkaTemplate.send("item-indexing-events", item.getId(), itemEvent);

            combinedText.append(item.getContent()).append("\n");
            count.incrementAndGet();
        });

        // Save lightweight Pod Index (Aggregated Text)
        podIndexRepository.save(new PodIndex(event.podId(), combinedText.toString()));

        job.setStatus(JobStatus.COMPLETED);
        job.setFinishedAt(Instant.now());
        jobRepository.save(job);

        log.info("Job {} split into {} item events. PodIndex created.", event.jobId(), count.get());
    }

    public void recover(PodIndexingEvent event, Exception e) {
        log.error("Retry exhausted for job {}", event.jobId(), e);
        throw new RuntimeException("Resilience4j Retry exhausted", e);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePermanentFailure(PodIndexingEvent event, Exception e) {
        IndexingJob job = jobRepository.findById(event.jobId());
        if (job != null) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(Instant.now());
            jobRepository.save(job);
        }
    }
}