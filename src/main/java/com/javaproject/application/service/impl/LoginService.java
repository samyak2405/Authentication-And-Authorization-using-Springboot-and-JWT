package com.javaproject.application.service.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.GoogleOAuthLoginRequest;
import com.javaproject.application.dto.request.LoginUserRequest;
import com.javaproject.application.dto.response.ApiResponse;
import com.javaproject.application.dto.response.LoginUserResponse;
import com.javaproject.application.security.authentication.AuthenticationResult;
import com.javaproject.application.security.authentication.AuthenticationStrategy;
import com.javaproject.application.security.authentication.AuthenticationStrategyFactory;
import com.javaproject.application.service.ProcessRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class LoginService implements ProcessRequest {

    private final AuthenticationStrategyFactory authenticationStrategyFactory;

    @Value("${app.auth.default-method:JWT}")
    private String defaultAuthMethod;

    /**
     * Authenticate the user and return the full AuthenticationResult
     * (used by the controller to set cookies and build the response).
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public AuthenticationResult authenticate(LoginUserRequest loginRequest) {
        String loginIdentifier = loginRequest.getEmail() != null && !loginRequest.getEmail().isBlank()
                ? loginRequest.getEmail()
                : loginRequest.getMobile();
        log.info("LoginService: Processing login request for identifier: {}", loginIdentifier);

        String authMethod = loginRequest.getAuthenticationMethod() != null
                ? loginRequest.getAuthenticationMethod()
                : defaultAuthMethod;

        AuthenticationStrategy strategy = authenticationStrategyFactory.getStrategy(authMethod);
        AuthenticationResult result = strategy.authenticate(loginRequest);

        log.info("LoginService: Login successful for identifier: {}", loginIdentifier);
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public AuthenticationResult authenticateGoogle(GoogleOAuthLoginRequest googleOAuthLoginRequest) {
        log.info("LoginService: Processing Google OAuth login request");
        AuthenticationStrategy strategy = authenticationStrategyFactory.getStrategy("GOOGLE");
        AuthenticationResult result = strategy.authenticate(googleOAuthLoginRequest);
        log.info("LoginService: Google OAuth login successful for email: {}", result.getEmail());
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ApiResponse<LoginUserResponse> processApiRequest(BaseRequest baseRequest) {
        LoginUserRequest loginRequest = (LoginUserRequest) baseRequest;
        AuthenticationResult result = authenticate(loginRequest);

        LoginUserResponse responseData = LoginUserResponse.builder()
                .email(result.getEmail())
                .mobile(result.getMobile())
                .roles(result.getRoles())
                .accessTokenExpiresAt(result.getAccessTokenExpiresAt())
                .build();

        ApiResponse<LoginUserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setRequestId(loginRequest.getRequestId());
        apiResponse.setSuccess(true);
        apiResponse.setResponseCode(String.valueOf(HttpStatus.OK.value()));
        apiResponse.setResponseMessage("Login successful");
        apiResponse.setTimestamp(OffsetDateTime.now());
        apiResponse.setData(responseData);

        return apiResponse;
    }
}
