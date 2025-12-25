package com.raghav.datahub.web.controller;

import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.service.pod.PodService;
import com.raghav.datahub.web.dto.AddDataRequest;
import com.raghav.datahub.web.dto.CreatePodRequest;
import com.raghav.datahub.web.dto.CreatePodResponse;
import com.raghav.datahub.web.dto.PodMetadataResponse;
import com.raghav.datahub.web.mapper.PodMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pods")
@RequiredArgsConstructor
public class PodController {

    private final PodService podService;
    private final PodMapper podMapper;

    @PostMapping
    public ResponseEntity<CreatePodResponse> createPod(@Valid @RequestBody CreatePodRequest request) {
        Pod pod = podService.createPod(request.name(), request.ownerUserId());
        return ResponseEntity.ok(new CreatePodResponse(pod.getId()));
    }

    @PostMapping("/{podId}/data")
    public ResponseEntity<Void> addData(
            @PathVariable String podId,
            @Valid @RequestBody AddDataRequest request) {
        podService.addData(podId, request.content());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{podId}")
    public ResponseEntity<PodMetadataResponse> getPod(@PathVariable String podId) {
        Pod pod = podService.getPod(podId);
        return ResponseEntity.ok(podMapper.toMetadataResponse(pod));
    }
}