package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.domain.model.PodIndex;
import com.raghav.datahub.domain.repository.PodIndexRepository;
import com.raghav.datahub.infrastructure.persistence.entity.PodIndexEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
@RequiredArgsConstructor
public class JpaPodIndexRepositoryAdapter implements PodIndexRepository {

    private final JpaPodIndexSpringRepository springRepository;

    @Override
    public void save(PodIndex index) {
        PodIndexEntity e = new PodIndexEntity();
        e.setPodId(index.getPodId());
        e.setCombinedText(index.getCombinedText());
        e.setCreatedAt(index.getCreatedAt());
        springRepository.save(e);
    }

    @Override
    public PodIndex findByPodId(String podId) {
        return springRepository.findById(podId)
                .map(e -> new PodIndex(e.getPodId(), e.getCombinedText(), e.getCreatedAt()))
                .orElse(null);
    }
}
