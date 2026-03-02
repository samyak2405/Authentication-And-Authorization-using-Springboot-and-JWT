package com.javaproject.application.validator.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.LoginUserRequest;
import com.javaproject.application.exception.custom.ApiValidationException;
import com.javaproject.application.validator.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRequestValidator implements Validator {

    @Value("${app.security.policy.enable:DEFAULT}")
    private String enableSecurityPolicy;
    @Override
    public void validateRequest(BaseRequest baseRequest) {
        LoginUserRequest loginUserRequest = (LoginUserRequest) baseRequest;
            if (loginUserRequest.getEmail() == null || loginUserRequest.getEmail().isEmpty()) {
                throw new ApiValidationException("Username cannot be null or empty.");
            }
            if (loginUserRequest.getPassword() == null || loginUserRequest.getPassword().isEmpty()) {
                throw new ApiValidationException("Password cannot be null or empty.");
            }
    }
}
