package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.domain.model.JobStatus;
import com.raghav.datahub.infrastructure.persistence.entity.IndexingJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaIndexingJobSpringRepository extends JpaRepository<IndexingJobEntity, String> {

    /**
     * Atomically update job status only if current status matches expected.
     * Returns number of rows updated (0 if condition not met).
     */
    @Modifying
    @Query("UPDATE IndexingJobEntity j SET j.status = :newStatus, j.startedAt = CURRENT_TIMESTAMP WHERE j.jobId = :id AND j.status = :expectedStatus")
    int updateStatusConditionally(@Param("id") String id, @Param("expectedStatus") JobStatus expectedStatus,
            @Param("newStatus") JobStatus newStatus);

    /**
     * Mark job as completed with finish time.
     */
    @Modifying
    @Query("UPDATE IndexingJobEntity j SET j.status = com.raghav.datahub.domain.model.JobStatus.COMPLETED, j.finishedAt = CURRENT_TIMESTAMP WHERE j.jobId = :id")
    int markAsCompleted(@Param("id") String id);

    /**
     * Mark job as failed with error message.
     */
    @Modifying
    @Query("UPDATE IndexingJobEntity j SET j.status = com.raghav.datahub.domain.model.JobStatus.FAILED, j.finishedAt = CURRENT_TIMESTAMP, j.errorMessage = :error WHERE j.jobId = :id")
    int markAsFailed(@Param("id") String id, @Param("error") String error);
}
