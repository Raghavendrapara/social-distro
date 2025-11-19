package com.raghav.datahub.infrastructure.repository;

import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.domain.repository.IndexingJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class InMemoryIndexingJobRepository implements IndexingJobRepository {

    private final Map<String, IndexingJob> jobs = new ConcurrentHashMap<>();

    @Override
    public IndexingJob save(IndexingJob job) {
        jobs.put(job.getJobId(), job);
        log.debug("Saved job {}", job.getJobId());
        return job;
    }

    @Override
    public IndexingJob findById(String jobId) {
        return jobs.get(jobId);
    }
}
