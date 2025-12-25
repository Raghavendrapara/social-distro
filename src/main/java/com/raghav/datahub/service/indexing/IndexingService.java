package com.raghav.datahub.service.indexing;

import com.raghav.datahub.domain.model.*;
import com.raghav.datahub.domain.repository.IndexingJobRepository;
import com.raghav.datahub.domain.repository.PodIndexRepository;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.infrastructure.persistence.entity.VectorChunkEntity;
import com.raghav.datahub.infrastructure.persistence.repository.JpaVectorChunkRepository;
import com.raghav.datahub.service.embedding.EmbeddingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IndexingService {

    private final PodRepository podRepository;
    private final PodIndexRepository podIndexRepository;
    private final JpaVectorChunkRepository vectorRepository;
    private final IndexingJobRepository jobRepository;
    private final ExecutorService executor;
    private final IndexingMetrics metrics;
    private final EmbeddingClient embeddingClient;

    private final long simulatedDelayMs;
    private final int chunkSize;
    private final long chunkTimeoutMs;
    private final int chunkMaxRetries;

    public IndexingService(PodRepository podRepository,
                           PodIndexRepository podIndexRepository,
                           JpaVectorChunkRepository vectorRepository, // Injected
                           IndexingJobRepository jobRepository,
                           ExecutorService indexingExecutorService,
                           IndexingMetrics metrics,
                           EmbeddingClient embeddingClient, // Injected
                           @Value("${datahub.indexing.simulated-delay-ms:1000}") long simulatedDelayMs,
                           @Value("${datahub.indexing.chunk-size:20}") int chunkSize,
                           @Value("${datahub.indexing.chunk-timeout-ms:5000}") long chunkTimeoutMs,
                           @Value("${datahub.indexing.chunk-max-retries:1}") int chunkMaxRetries) {
        this.podRepository = podRepository;
        this.podIndexRepository = podIndexRepository;
        this.vectorRepository = vectorRepository;
        this.jobRepository = jobRepository;
        this.executor = indexingExecutorService;
        this.metrics = metrics;
        this.embeddingClient = embeddingClient;
        this.simulatedDelayMs = simulatedDelayMs;
        this.chunkSize = chunkSize;
        this.chunkTimeoutMs = chunkTimeoutMs;
        this.chunkMaxRetries = chunkMaxRetries;
    }


    public IndexingJob startIndexing(String podId) {
        Pod pod = podRepository.findById(podId);
        if (pod == null) {
            throw new IllegalArgumentException("Pod not found: " + podId);
        }
        IndexingJob job = new IndexingJob(podId);
        jobRepository.save(job);
        metrics.incJobsStarted();
        metrics.incRunningJobs();
        executor.submit(() -> runIndexing(job));
        return job;
    }

    public IndexingJob getJob(String jobId) {
        return jobRepository.findById(jobId);
    }

    private void runIndexing(IndexingJob job) {
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);
        long startNs = System.nanoTime();

        try {
            Pod pod = podRepository.findById(job.getPodId());
            List<DataItem> items = new ArrayList<>(pod.getItems());
            List<List<DataItem>> chunks = chunk(items, chunkSize);

            log.info("Indexing pod {} with {} items", pod.getId(), items.size());

            List<CompletableFuture<Void>> futures = chunks.stream()
                    .map(chunk -> submitChunkWithRetry(job.getPodId(), chunk, 0))
                    .collect(Collectors.toList());

            // Wait for all
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            job.setStatus(JobStatus.COMPLETED);
            job.setFinishedAt(Instant.now());
            jobRepository.save(job);
            metrics.incJobsCompleted();

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
            metrics.addIndexingTime(elapsedMs);

        } catch (Exception e) {
            handleJobFailure(job, e);
        } finally {
            metrics.decRunningJobs();
        }
    }

    private CompletableFuture<Void> submitChunkWithRetry(String podId, List<DataItem> items, int attempt) {
        return CompletableFuture
                .runAsync(() -> processChunk(podId, items), executor)
                .orTimeout(chunkTimeoutMs, TimeUnit.MILLISECONDS)
                .handle((result, ex) -> {
                    if (ex == null) {
                        metrics.incChunksProcessed();
                        return CompletableFuture.completedFuture((Void) null);
                    } else {
                        metrics.incChunkFailures();
                        if (attempt >= chunkMaxRetries) {
                            CompletableFuture<Void> failed = new CompletableFuture<>();
                            failed.completeExceptionally(ex);
                            return failed;
                        } else {
                            metrics.incChunkRetries();
                            return submitChunkWithRetry(podId, items, attempt + 1);
                        }
                    }
                })
                .thenCompose(f -> f);
    }

    private void processChunk(String podId, List<DataItem> items) {

        String combinedText = items.stream()
                .map(DataItem::getContent)
                .collect(Collectors.joining("\n"));

        List<Double> embeddingList = embeddingClient.generateEmbedding(combinedText);

        if (!embeddingList.isEmpty()) {
            VectorChunkEntity entity = new VectorChunkEntity();
            entity.setPodId(podId);
            entity.setContent(combinedText);

            float[] floatArray = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                floatArray[i] = embeddingList.get(i).floatValue();
            }
            entity.setEmbedding(floatArray);

            vectorRepository.save(entity);
        }
    }

    private List<List<DataItem>> chunk(List<DataItem> items, int chunkSize) {
        List<List<DataItem>> chunks = new ArrayList<>();
        for (int i = 0; i < items.size(); i += chunkSize) {
            chunks.add(items.subList(i, Math.min(i + chunkSize, items.size())));
        }
        return chunks;
    }

    private void handleJobFailure(IndexingJob job, Throwable error) {
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(error.getMessage());
        job.setFinishedAt(Instant.now());
        jobRepository.save(job);
        metrics.incJobsFailed();
        log.error("Indexing failed", error);
    }
}