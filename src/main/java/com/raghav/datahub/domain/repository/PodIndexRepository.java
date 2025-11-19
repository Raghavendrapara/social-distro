package com.raghav.datahub.domain.repository;

import com.raghav.datahub.domain.model.PodIndex;

public interface PodIndexRepository {

    void save(PodIndex index);

    PodIndex findByPodId(String podId);
}
