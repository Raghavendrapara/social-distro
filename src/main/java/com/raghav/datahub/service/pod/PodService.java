package com.raghav.datahub.service.pod;

import com.raghav.datahub.domain.model.DataItem;
import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.domain.repository.PodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PodService {

    private final PodRepository podRepository;

    public Pod createPod(String name, String ownerUserId) {
        Pod pod = new Pod(name, ownerUserId);
        return podRepository.save(pod);
    }

    public Pod getPod(String podId) {
        Pod pod = podRepository.findById(podId);
        if (pod == null) {
            throw new IllegalArgumentException("Pod not found: " + podId);
        }
        return pod;
    }

    public void addData(String podId, String content) {
        Pod pod = getPod(podId);
        pod.addItem(new DataItem(content));
        podRepository.save(pod);
    }
}
