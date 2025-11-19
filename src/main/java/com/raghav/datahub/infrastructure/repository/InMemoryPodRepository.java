package com.raghav.datahub.infrastructure.repository;

import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.domain.repository.PodRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of PodRepository.
 *
 * This is an infrastructure adapter:
 *  - Uses ConcurrentHashMap for thread-safe storage
 *  - Can be replaced later by a DB-backed implementation
 */
@Slf4j
@Repository
public class InMemoryPodRepository implements PodRepository {

    private final Map<String, Pod> pods = new ConcurrentHashMap<>();

    @Override
    public Pod save(Pod pod) {
        if (pod == null) {
            throw new IllegalArgumentException("pod cannot be null");
        }
        pods.put(pod.getId(), pod);
        log.debug("Saved pod with id={}", pod.getId());
        return pod;
    }

    @Override
    public Pod findById(String id) {
        if (id == null) {
            return null;
        }
        return pods.get(id);
    }

    @Override
    public Collection<Pod> findAll() {
        return pods.values();
    }
}
