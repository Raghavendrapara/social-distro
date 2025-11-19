package com.raghav.datahub.service.indexing;

import com.raghav.datahub.domain.model.DataItem;
import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.domain.model.JobStatus;
import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.domain.model.PodIndex;
import com.raghav.datahub.domain.repository.IndexingJobRepository;
import com.raghav.datahub.domain.repository.PodIndexRepository;
import com.raghav.datahub.domain.repository.PodRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IndexingService {

    private final PodRepository podRepository;
    private final PodIndexRepository podIndexRepository;
    private final IndexingJobRepository indexingJobRepository;
    private final ExecutorService indexingExecutor;
    private final long simulatedDelayMs;

    public IndexingService(PodRepository podRepository,
                           PodIndexRepository podIndexRepository,
                           IndexingJobRepository indexingJobRepository,
                           ExecutorService indexingExecutorService,
                           @Value("${datahub.indexing.simulated-delay-ms:1000}") long simulatedDelayMs) {
        this.podRepository = podRepository;
        this.podIndexRepository = podIndexRepository;
        this.indexingJobRepository = indexingJobRepository;
        this.indexingExecutor = indexingExecutorService;
        this.simulatedDelayMs = simulatedDelayMs;
    }

    /**
     * Start an async indexing job for a pod.
     */
    public IndexingJob startIndexing(String podId) {
        Pod pod = podRepository.findById(podId);
        if (pod == null) {
            throw new IllegalArgumentException("Pod not found: " + podId);
        }

        IndexingJob job = new IndexingJob(podId);
        indexingJobRepository.save(job);

        indexingExecutor.submit(() -> runIndexing(job));

        return job;
    }

    /**
     * Get job status.
     */
    public IndexingJob getJob(String jobId) {
        return indexingJobRepository.findById(jobId);
    }

    /**
     * Actual work: build a PodIndex from Pod content.
     * Runs on a background thread.
     */
    private void runIndexing(IndexingJob job) {
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        indexingJobRepository.save(job);

        try {
            Pod pod = podRepository.findById(job.getPodId());
            if (pod == null) {
                throw new IllegalStateException("Pod not found during indexing: " + job.getPodId());
            }

            String combined = pod.getItems()
                    .stream()
                    .map(DataItem::getContent)
                    .collect(Collectors.joining("\n---\n"));

            // Simulate heavy processing time
            try {
                Thread.sleep(simulatedDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            PodIndex index = new PodIndex(pod.getId(), combined);
            podIndexRepository.save(index);

            job.setStatus(JobStatus.COMPLETED);
            log.info("Indexing completed for podId={} jobId={}", job.getPodId(), job.getJobId());
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            log.error("Indexing failed for jobId={}", job.getJobId(), e);
        } finally {
            job.setFinishedAt(Instant.now());
            indexingJobRepository.save(job);
        }
    }
}
