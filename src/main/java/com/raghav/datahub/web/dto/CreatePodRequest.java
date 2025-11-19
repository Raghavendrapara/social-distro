package com.raghav.datahub.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePodRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    private String ownerUserId;
}
