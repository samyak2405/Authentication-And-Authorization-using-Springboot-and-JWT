package com.javaproject.application.security.authentication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Unified authentication result returned by any authentication strategy.
 * Contains the tokens and user context regardless of how the user authenticated.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResult {

    private UUID userId;
    private String email;
    private String mobile;
    private String accessToken;
    private String refreshToken;
    private OffsetDateTime accessTokenExpiresAt;
    private OffsetDateTime refreshTokenExpiresAt;
    private List<String> roles;
    private String authenticationMethod;
}
