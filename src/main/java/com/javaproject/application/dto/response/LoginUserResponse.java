package com.javaproject.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUserResponse {

    private UUID userId;
    private String email;
    private String mobile;
    private List<String> roles;
    private OffsetDateTime accessTokenExpiresAt;
}
