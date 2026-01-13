package com.raghav.datahub.domain.repository;

import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.domain.model.JobStatus;

public interface IndexingJobRepository {

    IndexingJob save(IndexingJob job);

    IndexingJob findById(String jobId);

    /**
     * Atomically update job status only if current status matches expected.
     * 
     * @return true if update succeeded, false if condition not met
     */
    boolean updateStatusConditionally(String jobId, JobStatus expectedStatus, JobStatus newStatus);

    /**
     * Mark job as completed.
     */
    void markAsCompleted(String jobId);

    /**
     * Mark job as failed with error message.
     */
    void markAsFailed(String jobId, String errorMessage);
}
