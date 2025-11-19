package com.raghav.datahub.domain.repository;

import com.raghav.datahub.domain.model.Pod;

import java.util.Collection;

/**
 * Repository interface for Pod aggregate.
 *
 * This is a domain-level abstraction (port).
 * Implementations live in the infrastructure layer:
 *  - InMemoryPodRepository
 *  - JpaPodRepository
 *  - etc.
 */
public interface PodRepository {

    Pod save(Pod pod);

    Pod findById(String id);

    Collection<Pod> findAll();
}
