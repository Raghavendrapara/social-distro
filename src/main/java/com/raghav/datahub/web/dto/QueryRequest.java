package com.raghav.datahub.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QueryRequest {

    @NotBlank
    @Size(max = 500)   // reasonable question length
    private String question;
}
