package com.javaproject.application.security.authentication;

import com.javaproject.application.exception.custom.AccountNotVerifiedException;
import com.javaproject.application.exception.custom.ProcessApiException;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginSecurityService {

    private final UserRepository userRepository;

    public void validateAccountState(User user) {
        if (!user.isActive()) {
            throw new AccountNotVerifiedException(
                    "Please verify your account first.",
                    user.getEmail(),
                    user.getMobile()
            );
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new ProcessApiException("Account is locked until " + user.getLockedUntil(), HttpStatus.FORBIDDEN);
        }
        if (user.getAccountExpiresAt() != null && user.getAccountExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ProcessApiException("Account has expired", HttpStatus.FORBIDDEN);
        }
    }

    public void handleFailedLogin(User user, String identifier) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        user.setLastFailedLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        log.warn("Failed login attempt for user [{}]. Count: {}", identifier, user.getFailedLoginCount());
    }
}
