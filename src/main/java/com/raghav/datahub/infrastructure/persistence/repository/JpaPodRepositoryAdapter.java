package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.domain.model.DataItem;
import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.infrastructure.persistence.entity.DataItemEntity;
import com.raghav.datahub.infrastructure.persistence.entity.PodEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
@Primary
@RequiredArgsConstructor
public class JpaPodRepositoryAdapter implements PodRepository {

    private final JpaPodSpringRepository springRepository;
    private final DataItemSpringRepository dataItemSpringRepository;

    @Override
    public Pod save(Pod pod) {
        PodEntity entity = toEntity(pod);
        PodEntity saved = springRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Pod findById(String id) {
        return springRepository.findByIdWithItems(id)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public Collection<Pod> findAll() {
        List<PodEntity> entities = springRepository.findAll();
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private PodEntity toEntity(Pod pod) {
        PodEntity e = new PodEntity();
        e.setId(pod.getId());
        e.setName(pod.getName());
        e.setOwnerUserId(pod.getOwnerUserId());

        List<DataItemEntity> itemEntities = pod.getItems().stream()
                .map(item -> {
                    DataItemEntity di = new DataItemEntity();
                    di.setId(item.getId());
                    di.setContent(item.getContent());
                    di.setCreatedAt(item.getCreatedAt());
                    di.setPod(e); // back-reference
                    return di;
                })
                .collect(Collectors.toList());

        e.setItems(itemEntities);
        return e;
    }

    private Pod toDomain(PodEntity entity) {
        List<DataItem> items = entity.getItems().stream()
                .map(di -> new DataItem(
                        di.getId(),
                        di.getContent(),
                        di.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new Pod(
                entity.getId(),
                entity.getName(),
                entity.getOwnerUserId(),
                items
        );
    }

    @Override
    @Transactional(readOnly = true)
    public void streamItems(String podId, java.util.function.Consumer<DataItem> consumer) {

        try (Stream<DataItemEntity> stream = dataItemSpringRepository.streamByPodId(podId)) {
            stream.forEach(entity -> {
                consumer.accept(new DataItem(entity.getId(), entity.getContent(), entity.getCreatedAt()));
            });
        }
    }
}
