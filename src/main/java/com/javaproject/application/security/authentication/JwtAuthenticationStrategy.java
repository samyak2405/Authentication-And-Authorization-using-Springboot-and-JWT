package com.javaproject.application.security.authentication;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.LoginUserRequest;
import com.javaproject.application.exception.custom.ProcessApiException;
import com.javaproject.application.model.RefreshToken;
import com.javaproject.application.model.Role;
import com.javaproject.application.model.User;
import com.javaproject.application.model.UserRole;
import com.javaproject.application.repository.RefreshTokenRepository;
import com.javaproject.application.repository.UserRepository;
import com.javaproject.application.repository.UserRoleRepository;
import com.javaproject.application.security.jwt.JwtService;
import com.javaproject.application.util.PasswordUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT-based authentication strategy.
 * Validates email + password, then issues access and refresh tokens with embedded roles.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationStrategy implements AuthenticationStrategy {

    private static final String STRATEGY_NAME = "JWT";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final List<LoginCredentialStrategy> loginCredentialStrategies;

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public AuthenticationResult authenticate(BaseRequest request) {
        LoginUserRequest loginRequest = (LoginUserRequest) request;
        boolean hasEmailPassword = hasText(loginRequest.getEmail()) && hasText(loginRequest.getPassword());
        boolean hasMobileOtp = hasText(loginRequest.getMobile()) && hasText(loginRequest.getOtp());

        if (hasEmailPassword && hasMobileOtp) {
            throw new ProcessApiException(
                    "Provide either email+password or mobile+otp, not both.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!hasEmailPassword && !hasMobileOtp) {
            throw new ProcessApiException(
                    "Provide either email+password or mobile+otp.",
                    HttpStatus.BAD_REQUEST
            );
        }

        LoginCredentialStrategy loginCredentialStrategy = loginCredentialStrategies.stream()
                .filter(strategy -> strategy.supports(loginRequest))
                .findFirst()
                .orElseThrow(() -> new ProcessApiException(
                        "Unsupported login credential combination.",
                        HttpStatus.BAD_REQUEST
                ));

        User user = loginCredentialStrategy.authenticate(loginRequest);
        log.debug("Selected login credential strategy: {}", loginCredentialStrategy.getName());

        // Reset failed login count on success
        user.setFailedLoginCount(0);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        // Load roles
        List<String> roles = userRoleRepository.findByUser(user).stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .collect(Collectors.toList());

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), resolveTokenSubject(user), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // Persist refresh token
        persistRefreshToken(user, refreshToken);

        log.info("User [{}] authenticated via JWT", resolveUserIdentifier(user));

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

    private void persistRefreshToken(User user, String rawToken) {
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(PasswordUtility.hashToken(rawToken))
                .issuedAt(OffsetDateTime.now())
                .expiresAt(jwtService.getRefreshTokenExpiry())
                .build();
        refreshTokenRepository.save(entity);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String resolveUserIdentifier(User user) {
        if (hasText(user.getEmail())) {
            return user.getEmail();
        }
        return user.getMobile();
    }

    private String resolveTokenSubject(User user) {
        if (hasText(user.getEmail())) {
            return user.getEmail();
        }
        return user.getMobile();
    }
}
