package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.domain.model.DataItem;
import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.infrastructure.persistence.entity.DataItemEntity;
import com.raghav.datahub.infrastructure.persistence.entity.PodEntity;
import com.raghav.datahub.infrastructure.persistence.mapper.PodEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
@Primary
@RequiredArgsConstructor
public class JpaPodRepositoryAdapter implements PodRepository {

    private final JpaPodSpringRepository springRepository;
    private final DataItemSpringRepository dataItemSpringRepository;
    private final PodEntityMapper mapper;

    @Override
    public Pod save(Pod pod) {
        PodEntity entity = mapper.toEntity(pod); // One-line magic
        PodEntity saved = springRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Pod findById(String id) {
        return springRepository.findByIdWithItems(id)
                .map(mapper::toDomain)
                .orElse(null);
    }

    @Override
    public Collection<Pod> findAll() {
        return springRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void streamItems(String podId, java.util.function.Consumer<DataItem> consumer) {
        try (Stream<DataItemEntity> stream = dataItemSpringRepository.streamByPodId(podId)) {
            stream.forEach(entity -> {
                consumer.accept(mapper.toItemDomain(entity));
            });
        }
    }
}