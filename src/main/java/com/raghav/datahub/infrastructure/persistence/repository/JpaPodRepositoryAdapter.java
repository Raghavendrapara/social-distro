package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.infrastructure.persistence.entity.PodEntity;
import com.raghav.datahub.infrastructure.persistence.mapper.PodEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@Primary
@RequiredArgsConstructor
public class JpaPodRepositoryAdapter implements PodRepository {

    private final JpaPodSpringRepository springRepository;
    private final PodEntityMapper mapper;

    @Override
    public Pod save(Pod pod) {
        PodEntity entity = mapper.toEntity(pod);
        entity.setItems(mapper.toEntityItems(pod, entity));
        PodEntity saved = springRepository.save(entity);

        Pod domain = mapper.toDomain(saved);
        domain.getItems().addAll(mapper.toDomainItems(saved.getItems()));
        return domain;
    }

    @Override
    public Pod findById(String id) {
        return springRepository.findById(id)
                .map(entity -> {
                    Pod pod = mapper.toDomain(entity);
                    pod.getItems().addAll(mapper.toDomainItems(entity.getItems()));
                    return pod;
                })
                .orElse(null);
    }

    @Override
    public Collection<Pod> findAll() {
        List<PodEntity> entities = springRepository.findAll();
        return entities.stream()
                .map(entity -> {
                    Pod pod = mapper.toDomain(entity);
                    pod.getItems().addAll(mapper.toDomainItems(entity.getItems()));
                    return pod;
                })
                .toList();
    }
}
