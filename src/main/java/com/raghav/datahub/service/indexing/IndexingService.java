package com.raghav.datahub.service.indexing;

import com.raghav.datahub.domain.model.*;
import com.raghav.datahub.domain.repository.IndexingJobRepository;
import com.raghav.datahub.domain.repository.PodIndexRepository;
import com.raghav.datahub.domain.repository.PodRepository;
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
    private final IndexingJobRepository jobRepository;
    private final ExecutorService executor;
    private final IndexingMetrics metrics;

    private final long simulatedDelayMs;
    private final int chunkSize;
    private final long chunkTimeoutMs;
    private final int chunkMaxRetries;

    public IndexingService(PodRepository podRepository,
                           PodIndexRepository podIndexRepository,
                           IndexingJobRepository jobRepository,
                           ExecutorService indexingExecutorService,
                           IndexingMetrics metrics,
                           @Value("${datahub.indexing.simulated-delay-ms:1000}") long simulatedDelayMs,
                           @Value("${datahub.indexing.chunk-size:20}") int chunkSize,
                           @Value("${datahub.indexing.chunk-timeout-ms:2000}") long chunkTimeoutMs,
                           @Value("${datahub.indexing.chunk-max-retries:1}") int chunkMaxRetries) {
        this.podRepository = podRepository;
        this.podIndexRepository = podIndexRepository;
        this.jobRepository = jobRepository;
        this.executor = indexingExecutorService;
        this.metrics = metrics;
        this.simulatedDelayMs = simulatedDelayMs;
        this.chunkSize = chunkSize;
        this.chunkTimeoutMs = chunkTimeoutMs;
        this.chunkMaxRetries = chunkMaxRetries;
    }

    // -------------------------------
    // PUBLIC API
    // -------------------------------

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

    // -------------------------------
    // INTERNAL PARALLEL INDEXING LOGIC
    // -------------------------------

    private void runIndexing(IndexingJob job) {
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        long startNs = System.nanoTime();

        try {
            Pod pod = podRepository.findById(job.getPodId());
            if (pod == null) {
                throw new IllegalStateException("Pod not found during indexing: " + job.getPodId());
            }

            List<DataItem> items = new ArrayList<>(pod.getItems());
            List<List<DataItem>> chunks = chunk(items, chunkSize);

            log.info("Indexing pod {} with {} items in {} chunks", pod.getId(), items.size(), chunks.size());

            // Build futures with retry + timeout
            List<CompletableFuture<String>> futures = chunks.stream()
                    .map(chunk -> submitChunkWithRetry(chunk, 0))
                    .collect(Collectors.toList());

            // Wait for all futures
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            // Gather results or propagate error
            List<String> processedChunks = allFutures
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join) // join is safe here after allOf
                            .collect(Collectors.toList())
                    )
                    .join(); // can throw CompletionException

            String finalIndexedText = String.join("\n---\n", processedChunks);

            PodIndex index = new PodIndex(pod.getId(), finalIndexedText);
            podIndexRepository.save(index);

            job.setStatus(JobStatus.COMPLETED);
            job.setFinishedAt(Instant.now());
            jobRepository.save(job);

            metrics.incJobsCompleted();

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
            metrics.addIndexingTime(elapsedMs);

            log.info("Indexing completed for pod {} in {} ms", pod.getId(), elapsedMs);

        } catch (CompletionException ce) {
            // An async task failed — unwrap cause for logging
            Throwable cause = ce.getCause() != null ? ce.getCause() : ce;
            handleJobFailure(job, cause);
        } catch (Exception e) {
            handleJobFailure(job, e);
        } finally {
            metrics.decRunningJobs();
        }
    }

    // -------------------------------
    // CHUNK SUBMISSION & RETRIES
    // -------------------------------

    private CompletableFuture<String> submitChunkWithRetry(List<DataItem> items, int attempt) {
        return CompletableFuture
                .supplyAsync(() -> processChunk(items), executor)
                .orTimeout(chunkTimeoutMs, TimeUnit.MILLISECONDS)
                .handle((result, ex) -> {
                    if (ex == null) {
                        // success
                        metrics.incChunksProcessed();
                        return CompletableFuture.completedFuture(result);
                    } else {
                        metrics.incChunkFailures();
                        if (attempt >= chunkMaxRetries) {
                            // give up, propagate failure
                            log.error("Chunk failed after {} attempts", attempt + 1, ex);
                            // fail the future chain
                            CompletableFuture<String> failed = new CompletableFuture<>();
                            failed.completeExceptionally(ex);
                            return failed;
                        } else {
                            // retry
                            metrics.incChunkRetries();
                            log.warn("Chunk failed on attempt {}. Retrying...", attempt + 1, ex);
                            return submitChunkWithRetry(items, attempt + 1);
                        }
                    }
                })
                .thenCompose(f -> f); // flatten nested future
    }

    // -------------------------------
    // LOW-LEVEL CHUNK PROCESSING
    // -------------------------------

    private List<List<DataItem>> chunk(List<DataItem> items, int chunkSize) {
        List<List<DataItem>> chunks = new ArrayList<>();
        for (int i = 0; i < items.size(); i += chunkSize) {
            chunks.add(items.subList(i, Math.min(i + chunkSize, items.size())));
        }
        return chunks;
    }

    private String processChunk(List<DataItem> items) {
        // Simulate heavy work
        try {
            Thread.sleep(simulatedDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Chunk processing interrupted", e);
        }

        return items.stream()
                .map(DataItem::getContent)
                .collect(Collectors.joining(" "));
    }

    private void handleJobFailure(IndexingJob job, Throwable error) {
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(error.getMessage());
        job.setFinishedAt(Instant.now());
        jobRepository.save(job);

        metrics.incJobsFailed();

        log.error("Indexing failed for job {} (podId={})", job.getJobId(), job.getPodId(), error);
    }
}
