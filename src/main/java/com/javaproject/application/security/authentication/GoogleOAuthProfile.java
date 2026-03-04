package com.javaproject.application.security.authentication;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoogleOAuthProfile {
    private final String subject;
    private final String email;
    private final boolean emailVerified;
    private final String displayName;
    private final String pictureUrl;
}
