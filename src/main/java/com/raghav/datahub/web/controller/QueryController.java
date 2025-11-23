package com.raghav.datahub.web.controller;

import com.raghav.datahub.service.query.QueryService;
import com.raghav.datahub.web.dto.QueryRequest;
import com.raghav.datahub.web.dto.QueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pods/{podId}/query")
public class QueryController {

    private final QueryService queryService;

    @PostMapping
    public ResponseEntity<QueryResponse> query(
            @PathVariable String podId,
            @RequestBody QueryRequest req
    ) {
        QueryResponse response = queryService.queryPod(podId, req);
        return ResponseEntity.ok(response);
    }
}
