package com.javaproject.application.validator.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.ResendRegistrationOtpRequest;
import com.javaproject.application.validator.Validator;
import org.springframework.stereotype.Component;

@Component
public class ResendRegistrationOtpValidator implements Validator {

    @Override
    public void validateRequest(BaseRequest baseRequest) {
        ResendRegistrationOtpRequest request = (ResendRegistrationOtpRequest) baseRequest;
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
    }
}
