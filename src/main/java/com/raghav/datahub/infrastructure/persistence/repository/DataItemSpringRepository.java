package com.raghav.datahub.infrastructure.persistence.repository;

import com.raghav.datahub.infrastructure.persistence.entity.DataItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import java.util.stream.Stream;


public interface DataItemSpringRepository extends JpaRepository<DataItemEntity, String> {

    @QueryHints(value = @QueryHint(name ="jakarta.persistence.query.fetchSize" , value = "500"))
    Stream<DataItemEntity> streamByPodId(String podId);
}