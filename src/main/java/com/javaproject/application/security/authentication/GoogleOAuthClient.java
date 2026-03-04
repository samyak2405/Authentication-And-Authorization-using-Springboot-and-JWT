package com.javaproject.application.security.authentication;

import com.javaproject.application.exception.custom.ProcessApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class GoogleOAuthClient {

    private static final String GOOGLE_AUDIENCE_ERROR = "google-id-token-audience-mismatch";

    private final RestClient restClient;
    private final JwtDecoder googleIdTokenDecoder;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUri;

    public GoogleOAuthClient(
            @Value("${app.oauth.google.client-id:}") String clientId,
            @Value("${app.oauth.google.client-secret:}") String clientSecret,
            @Value("${app.oauth.google.token-uri:https://oauth2.googleapis.com/token}") String tokenUri,
            @Value("${app.oauth.google.jwk-set-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwkSetUri,
            @Value("${app.oauth.google.issuer:https://accounts.google.com}") String issuer
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUri = tokenUri;
        this.restClient = RestClient.builder().build();

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            if (token.getAudience() != null && token.getAudience().contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(GOOGLE_AUDIENCE_ERROR, "Invalid Google token audience", null)
            );
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        this.googleIdTokenDecoder = decoder;
    }

    public GoogleOAuthProfile exchangeCodeAndValidate(
            String authorizationCode,
            String redirectUri,
            String codeVerifier,
            String expectedNonce
    ) {
        if (isBlank(clientId) || isBlank(clientSecret)) {
            throw new ProcessApiException("Google OAuth is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", authorizationCode);
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("redirect_uri", redirectUri);
        formData.add("grant_type", "authorization_code");
        if (!isBlank(codeVerifier)) {
            formData.add("code_verifier", codeVerifier);
        }

        GoogleOAuthTokenResponse tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(GoogleOAuthTokenResponse.class);
        } catch (RestClientException ex) {
            log.warn("Google token exchange failed: {}", ex.getMessage());
            throw new ProcessApiException("Failed to exchange Google authorization code.", HttpStatus.UNAUTHORIZED);
        }

        if (tokenResponse == null || isBlank(tokenResponse.getIdToken())) {
            throw new ProcessApiException("Google did not return a valid ID token.", HttpStatus.UNAUTHORIZED);
        }

        Jwt jwt;
        try {
            jwt = googleIdTokenDecoder.decode(tokenResponse.getIdToken());
        } catch (JwtException ex) {
            throw new ProcessApiException("Invalid Google ID token.", HttpStatus.UNAUTHORIZED);
        }

        if (!isBlank(expectedNonce)) {
            String actualNonce = jwt.getClaimAsString("nonce");
            if (isBlank(actualNonce) || !expectedNonce.equals(actualNonce)) {
                throw new ProcessApiException("Invalid Google token nonce.", HttpStatus.UNAUTHORIZED);
            }
        }

        String subject = jwt.getSubject();
        String email = normalize(jwt.getClaimAsString("email"));
        boolean emailVerified = toBoolean(jwt.getClaim("email_verified"));
        String displayName = normalize(jwt.getClaimAsString("name"));
        String pictureUrl = normalize(jwt.getClaimAsString("picture"));

        if (isBlank(subject) || isBlank(email)) {
            throw new ProcessApiException("Google account does not include required identity claims.", HttpStatus.UNAUTHORIZED);
        }
        if (!emailVerified) {
            throw new ProcessApiException("Google email must be verified.", HttpStatus.UNAUTHORIZED);
        }

        return GoogleOAuthProfile.builder()
                .subject(subject)
                .email(email)
                .emailVerified(true)
                .displayName(displayName)
                .pictureUrl(pictureUrl)
                .build();
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return false;
    }

    private String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
