package com.raghav.datahub.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddDataRequest(
        @NotBlank @Size(max = 10_000) String content
) {}