package com.raghav.datahub.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddDataRequest {

    @NotBlank
    @Size(max = 10_000)   // prevent megabyte spam
    private String content;
}
