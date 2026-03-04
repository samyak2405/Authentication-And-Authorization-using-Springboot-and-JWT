package com.javaproject.application.security.authentication;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.GoogleOAuthLoginRequest;
import com.javaproject.application.exception.custom.ProcessApiException;
import com.javaproject.application.model.OAuthIdentity;
import com.javaproject.application.model.RefreshToken;
import com.javaproject.application.model.Role;
import com.javaproject.application.model.User;
import com.javaproject.application.model.UserRole;
import com.javaproject.application.repository.OAuthIdentityRepository;
import com.javaproject.application.repository.RefreshTokenRepository;
import com.javaproject.application.repository.UserRepository;
import com.javaproject.application.repository.UserRoleRepository;
import com.javaproject.application.security.jwt.JwtService;
import com.javaproject.application.util.PasswordUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthAuthenticationStrategy implements AuthenticationStrategy {

    private static final String STRATEGY_NAME = "GOOGLE";
    private static final String PROVIDER_NAME = "GOOGLE";

    private final GoogleOAuthClient googleOAuthClient;
    private final OAuthIdentityRepository oAuthIdentityRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserLoginSecurityService userLoginSecurityService;

    @Value("${app.oauth.google.enabled:false}")
    private boolean googleOAuthEnabled;

    @Value("${app.security.password.algo:BCRYPT}")
    private String passwordAlgo;

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public AuthenticationResult authenticate(BaseRequest request) {
        if (!googleOAuthEnabled) {
            throw new ProcessApiException("Google OAuth login is disabled.", HttpStatus.FORBIDDEN);
        }
        if (!(request instanceof GoogleOAuthLoginRequest googleRequest)) {
            throw new ProcessApiException("Invalid request for Google OAuth authentication.", HttpStatus.BAD_REQUEST);
        }
        if (!PROVIDER_NAME.equalsIgnoreCase(googleRequest.getProvider())) {
            throw new ProcessApiException("Unsupported OAuth provider.", HttpStatus.BAD_REQUEST);
        }

        GoogleOAuthProfile googleProfile = googleOAuthClient.exchangeCodeAndValidate(
                googleRequest.getAuthorizationCode(),
                googleRequest.getRedirectUri(),
                googleRequest.getCodeVerifier(),
                googleRequest.getNonce()
        );

        OffsetDateTime now = OffsetDateTime.now();
        OAuthIdentity identity = oAuthIdentityRepository
                .findByProviderAndProviderSubject(PROVIDER_NAME, googleProfile.getSubject())
                .orElse(null);

        User user;
        if (identity != null) {
            user = identity.getUser();
            userLoginSecurityService.validateAccountState(user);
            upsertIdentity(identity, googleProfile, now);
        } else {
            userRepository.getByEmail(googleProfile.getEmail()).ifPresent(existing -> {
                throw new ProcessApiException(
                        "Google sign-in is not allowed because the email already exists.",
                        HttpStatus.CONFLICT
                );
            });
            user = createOAuthUser(googleProfile, now);
            identity = createOAuthIdentity(user, googleProfile, now);
            oAuthIdentityRepository.save(identity);
        }

        user.setFailedLoginCount(0);
        user.setLastLoginAt(now);
        userRepository.save(user);

        List<String> roles = userRoleRepository.findByUser(user).stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .collect(Collectors.toList());

        String accessToken = jwtService.generateAccessToken(user.getId(), resolveTokenSubject(user), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        persistRefreshToken(user, refreshToken, googleRequest);

        return AuthenticationResult.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresAt(jwtService.getAccessTokenExpiry())
                .refreshTokenExpiresAt(jwtService.getRefreshTokenExpiry())
                .roles(roles)
                .authenticationMethod(STRATEGY_NAME)
                .build();
    }

    private User createOAuthUser(GoogleOAuthProfile profile, OffsetDateTime now) {
        User user = User.builder()
                .email(profile.getEmail())
                .mobile(null)
                .passwordHash(PasswordUtility.hashPassword("OAUTH_ONLY:" + UUID.randomUUID()))
                .passwordAlgo(passwordAlgo)
                .isActive(true)
                .accountExpiresAt(null)
                .lockedUntil(null)
                .lockReason(null)
                .isMfaEnabled(false)
                .mfaMethod(null)
                .authenticationMethod(STRATEGY_NAME)
                .failedLoginCount(0)
                .lastFailedLoginAt(null)
                .lastLoginAt(now)
                .passwordChangedAt(now)
                .passwordExpiresAt(null)
                .mustChangePassword(false)
                .securityPolicy(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return userRepository.save(user);
    }

    private OAuthIdentity createOAuthIdentity(User user, GoogleOAuthProfile profile, OffsetDateTime now) {
        return OAuthIdentity.builder()
                .user(user)
                .provider(PROVIDER_NAME)
                .providerSubject(profile.getSubject())
                .email(profile.getEmail())
                .emailVerified(profile.isEmailVerified())
                .displayName(profile.getDisplayName())
                .pictureUrl(profile.getPictureUrl())
                .profileSyncedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void upsertIdentity(OAuthIdentity identity, GoogleOAuthProfile profile, OffsetDateTime now) {
        identity.setEmail(profile.getEmail());
        identity.setEmailVerified(profile.isEmailVerified());
        identity.setDisplayName(profile.getDisplayName());
        identity.setPictureUrl(profile.getPictureUrl());
        identity.setProfileSyncedAt(now);
        identity.setUpdatedAt(now);
        oAuthIdentityRepository.save(identity);
    }

    private void persistRefreshToken(User user, String rawToken, GoogleOAuthLoginRequest request) {
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(PasswordUtility.hashToken(rawToken))
                .issuedAt(OffsetDateTime.now())
                .expiresAt(jwtService.getRefreshTokenExpiry())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .build();
        refreshTokenRepository.save(entity);
    }

    private String resolveTokenSubject(User user) {
        if (hasText(user.getEmail())) {
            return user.getEmail();
        }
        return user.getMobile();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
