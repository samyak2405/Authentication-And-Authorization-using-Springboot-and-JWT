package com.javaproject.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendRegistrationOtpResponse {
    private UUID userId;
    private String email;
    private String mobile;
    private OffsetDateTime otpExpiresAt;
}
