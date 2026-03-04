package com.javaproject.application.validator.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.ResendRegistrationOtpRequest;
import com.javaproject.application.validator.Validator;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ResendRegistrationOtpValidator implements Validator {

    @Override
    public void validateRequest(BaseRequest baseRequest) {
        ResendRegistrationOtpRequest request = (ResendRegistrationOtpRequest) baseRequest;
        if (isBlank(request.getEmail()) && isBlank(request.getMobile())) {
            throw new IllegalArgumentException("Either email or mobile is required.");
        }
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.trim().isEmpty();
    }
}
