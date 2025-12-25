package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.infrastructure.persistence.entity.PodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPodSpringRepository extends JpaRepository<PodEntity, String> {

    @Query("SELECT p FROM PodEntity p LEFT JOIN FETCH p.items WHERE p.id = :id")
    Optional<PodEntity> findByIdWithItems(@Param("id") String id);
}