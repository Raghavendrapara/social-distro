package com.raghav.datahub.infrastructure.repository;

import com.raghav.datahub.domain.model.PodIndex;
import com.raghav.datahub.domain.repository.PodIndexRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPodIndexRepository implements PodIndexRepository {

    private final Map<String, PodIndex> indexes = new ConcurrentHashMap<>();

    @Override
    public void save(PodIndex index) {
        indexes.put(index.getPodId(), index);
    }

    @Override
    public PodIndex findByPodId(String podId) {
        return indexes.get(podId);
    }
}
