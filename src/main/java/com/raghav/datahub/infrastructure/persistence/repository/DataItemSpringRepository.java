package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.infrastructure.persistence.entity.DataItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import java.util.stream.Stream;

import static org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE;

public interface DataItemSpringRepository extends JpaRepository<DataItemEntity, String> {

    @QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = "500"))
    Stream<DataItemEntity> streamByPodId(String podId);
}