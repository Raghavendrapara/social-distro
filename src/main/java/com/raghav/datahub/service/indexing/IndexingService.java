package com.raghav.datahub.service.indexing;

import com.raghav.datahub.domain.model.*;
import com.raghav.datahub.domain.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IndexingService {

    private final PodRepository podRepository;
    private final PodIndexRepository podIndexRepository;
    private final IndexingJobRepository jobRepository;
    private final ExecutorService executor;

    private final long simulatedDelayMs;
    private final int chunkSize;

    public IndexingService(PodRepository podRepository,
                           PodIndexRepository podIndexRepository,
                           IndexingJobRepository jobRepository,
                           ExecutorService indexingExecutorService,
                           @Value("${datahub.indexing.simulated-delay-ms:1000}") long simulatedDelayMs,
                           @Value("${datahub.indexing.chunk-size:20}") int chunkSize) {
        this.podRepository = podRepository;
        this.podIndexRepository = podIndexRepository;
        this.jobRepository = jobRepository;
        this.executor = indexingExecutorService;
        this.simulatedDelayMs = simulatedDelayMs;
        this.chunkSize = chunkSize;
    }

    
    // PUBLIC API

    public IndexingJob startIndexing(String podId) {
        Pod pod = podRepository.findById(podId);
        if (pod == null) {
            throw new IllegalArgumentException("Pod not found: " + podId);
        }

        IndexingJob job = new IndexingJob(podId);
        jobRepository.save(job);

        executor.submit(() -> runIndexing(job));

        return job;
    }

    public IndexingJob getJob(String jobId) {
        return jobRepository.findById(jobId);
    }

    
    // INTERNAL PARALLEL INDEXING LOGIC

    private void runIndexing(IndexingJob job) {
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        try {
            Pod pod = podRepository.findById(job.getPodId());
            List<DataItem> items = new ArrayList<>(pod.getItems());

            // 1. Chunk the data
            List<List<DataItem>> chunks = chunk(items, chunkSize);
            log.info("Indexing pod {} with {} chunks", pod.getId(), chunks.size());

            // 2. Submit each chunk as a parallel future
            List<CompletableFuture<String>> futures = chunks.stream()
                    .map(chunk -> CompletableFuture.supplyAsync(() -> processChunk(chunk), executor))
                    .collect(Collectors.toList());

            // 3. Wait for all futures
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            // 4. Combine results
            CompletableFuture<List<String>> combined = allFutures.thenApply(v ->
                    futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList())
            );

            List<String> processedChunks = combined.join();

            // 5. Merge into final index
            String finalIndexedText = String.join("\n---\n", processedChunks);

            PodIndex index = new PodIndex(pod.getId(), finalIndexedText);
            podIndexRepository.save(index);

            job.setStatus(JobStatus.COMPLETED);
            job.setFinishedAt(Instant.now());
            jobRepository.save(job);

            log.info("Indexing completed for pod {}", pod.getId());

        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(Instant.now());
            jobRepository.save(job);

            log.error("Indexing failed for job {}", job.getJobId(), e);
        }
    }

    // PROCESSING HELPERS

    private List<List<DataItem>> chunk(List<DataItem> items, int chunkSize) {
        List<List<DataItem>> chunks = new ArrayList<>();
        for (int i = 0; i < items.size(); i += chunkSize) {
            chunks.add(items.subList(i, Math.min(i + chunkSize, items.size())));
        }
        return chunks;
    }

    private String processChunk(List<DataItem> items) {
        try {
            Thread.sleep(simulatedDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return items.stream()
                .map(DataItem::getContent)
                .collect(Collectors.joining(" "));
    }
}
