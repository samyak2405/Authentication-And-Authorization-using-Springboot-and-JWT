package com.javaproject.application.security.authentication;

import com.javaproject.application.dto.request.LoginUserRequest;
import com.javaproject.application.exception.custom.ProcessApiException;
import com.javaproject.application.exception.custom.UserNotFoundException;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.UserRepository;
import com.javaproject.application.util.PasswordUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailPasswordLoginCredentialStrategy implements LoginCredentialStrategy {

    private final UserRepository userRepository;
    private final UserLoginSecurityService userLoginSecurityService;

    @Override
    public String getName() {
        return "EMAIL_PASSWORD";
    }

    @Override
    public boolean supports(LoginUserRequest loginRequest) {
        return hasText(loginRequest.getEmail()) && hasText(loginRequest.getPassword());
    }

    @Override
    public User authenticate(LoginUserRequest loginRequest) {
        User user = userRepository.getByEmail(loginRequest.getEmail().trim())
                .orElseThrow(() -> new UserNotFoundException("Invalid email or password"));
        userLoginSecurityService.validateAccountState(user);
        if (isGoogleOnlyAccount(user)) {
            throw new ProcessApiException(
                    "This account uses Google sign-in. Use Google login or set a password first.",
                    HttpStatus.FORBIDDEN
            );
        }
        if (!PasswordUtility.verifyPassword(loginRequest.getPassword(), user.getPasswordHash())) {
            userLoginSecurityService.handleFailedLogin(user, loginRequest.getEmail());
            throw new ProcessApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    private boolean isGoogleOnlyAccount(User user) {
        return hasText(user.getAuthenticationMethod())
                && "GOOGLE".equalsIgnoreCase(user.getAuthenticationMethod());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
