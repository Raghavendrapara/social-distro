package com.raghav.datahub.service.indexing;

import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.domain.model.JobStatus;
import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.domain.repository.IndexingJobRepository;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.service.indexing.event.PodIndexingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {

    private final PodRepository podRepository;
    private final IndexingJobRepository jobRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IndexingMetrics metrics;

    private static final String TOPIC = "pod-indexing-jobs";

    @Transactional
    public IndexingJob startIndexing(String podId) {
        Pod pod = podRepository.findById(podId);
        if (pod == null) {
            throw new IllegalArgumentException("Pod not found: " + podId);
        }

        IndexingJob job = new IndexingJob(podId);
        job.setStatus(JobStatus.PENDING);
        jobRepository.save(job);

        String traceId = UUID.randomUUID().toString();
        PodIndexingEvent event = new PodIndexingEvent(job.getJobId(), podId, traceId);

        kafkaTemplate.send(TOPIC, podId, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Dispatched indexing job {} for pod {}", job.getJobId(), podId);
                        metrics.incJobsStarted();
                    } else {
                        log.error("Failed to dispatch indexing job", ex);
                    }
                });

        return job;
    }

    public IndexingJob getJob(String jobId) {
        return jobRepository.findById(jobId);
    }
}