package com.raghav.datahub.web.controller;

import com.raghav.datahub.service.query.QueryService;
import com.raghav.datahub.web.dto.QueryRequest;
import com.raghav.datahub.web.dto.QueryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    public ResponseEntity<QueryResponse> queryPod(
            @PathVariable String podId,
            @Valid @RequestBody QueryRequest request) {
        QueryResponse response = queryService.queryPod(podId, request);
        return ResponseEntity.ok(response);
    }
}
