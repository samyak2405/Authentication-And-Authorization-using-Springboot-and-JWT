package com.javaproject.application.validator.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.VerifyRegistrationOtpRequest;
import com.javaproject.application.validator.Validator;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class VerifyRegistrationOtpValidator implements Validator {

    @Override
    public void validateRequest(BaseRequest baseRequest) {
        VerifyRegistrationOtpRequest request = (VerifyRegistrationOtpRequest) baseRequest;
        if (isBlank(request.getEmail()) && isBlank(request.getMobile())) {
            throw new IllegalArgumentException("Either email or mobile is required.");
        }
        if (request.getOtp() == null || request.getOtp().isBlank()) {
            throw new IllegalArgumentException("OTP is required");
        }
        if (!request.getOtp().matches("\\d{6}")) {
            throw new IllegalArgumentException("OTP must be exactly 6 digits");
        }
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.trim().isEmpty();
    }
}
