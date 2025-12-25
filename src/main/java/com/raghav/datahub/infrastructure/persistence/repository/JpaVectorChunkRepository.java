package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.infrastructure.persistence.entity.VectorChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaVectorChunkRepository extends JpaRepository<VectorChunkEntity, String> {
}