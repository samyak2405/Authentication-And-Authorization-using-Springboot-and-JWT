package com.javaproject.application.security.authentication;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.LoginUserRequest;
import com.javaproject.application.exception.custom.ProcessApiException;
import com.javaproject.application.exception.custom.UserNotFoundException;
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
import java.util.UUID;
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

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public AuthenticationResult authenticate(BaseRequest request) {
        LoginUserRequest loginRequest = (LoginUserRequest) request;

        // 1. Lookup user
        User user = userRepository.getByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Invalid email or password"));

        // 2. Check account state
        validateAccountState(user);

        // 3. Verify password
        if (!PasswordUtility.verifyPassword(loginRequest.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new ProcessApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        // 4. Reset failed login count on success
        user.setFailedLoginCount(0);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        // 5. Load roles
        List<String> roles = userRoleRepository.findByUser(user).stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .collect(Collectors.toList());

        // 6. Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 7. Persist refresh token
        persistRefreshToken(user, refreshToken);

        log.info("User [{}] authenticated via JWT", user.getEmail());

        return AuthenticationResult.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresAt(jwtService.getAccessTokenExpiry())
                .refreshTokenExpiresAt(jwtService.getRefreshTokenExpiry())
                .roles(roles)
                .authenticationMethod(STRATEGY_NAME)
                .build();
    }

    private void validateAccountState(User user) {
        if (!user.isActive()) {
            throw new ProcessApiException("Account is deactivated", HttpStatus.FORBIDDEN);
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new ProcessApiException("Account is locked until " + user.getLockedUntil(), HttpStatus.FORBIDDEN);
        }
        if (user.getAccountExpiresAt() != null && user.getAccountExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ProcessApiException("Account has expired", HttpStatus.FORBIDDEN);
        }
    }

    private void handleFailedLogin(User user) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        user.setLastFailedLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        log.warn("Failed login attempt for user [{}]. Count: {}", user.getEmail(), user.getFailedLoginCount());
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
}
