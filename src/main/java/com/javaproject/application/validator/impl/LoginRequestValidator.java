package com.javaproject.application.validator.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.LoginUserRequest;
import com.javaproject.application.exception.custom.ApiValidationException;
import com.javaproject.application.validator.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRequestValidator implements Validator {

    @Value("${app.security.policy.enable:DEFAULT}")
    private String enableSecurityPolicy;
    @Override
    public void validateRequest(BaseRequest baseRequest) {
        LoginUserRequest loginUserRequest = (LoginUserRequest) baseRequest;
        boolean hasEmailPassword = isNotBlank(loginUserRequest.getEmail()) && isNotBlank(loginUserRequest.getPassword());
        boolean hasMobileOtp = isNotBlank(loginUserRequest.getMobile()) && isNotBlank(loginUserRequest.getOtp());

        if (!hasEmailPassword && !hasMobileOtp) {
            throw new ApiValidationException("Provide either email+password or mobile+otp.");
        }
    }

    private boolean isNotBlank(String value) {
        return !Objects.isNull(value) && !value.trim().isEmpty();
    }
}
