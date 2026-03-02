package com.javaproject.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseRequest {

    @NotBlank
    @Size(max = 64)
    private String requestId;

    @Size(max = 64)
    private String ipAddress;

    @Size(max = 255)
    private String userAgent;
}
