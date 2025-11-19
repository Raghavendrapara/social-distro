package com.raghav.datahub.domain.repository;

import com.raghav.datahub.domain.model.IndexingJob;

public interface IndexingJobRepository {

    IndexingJob save(IndexingJob job);

    IndexingJob findById(String jobId);
}
