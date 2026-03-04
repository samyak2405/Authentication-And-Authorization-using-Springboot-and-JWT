package com.javaproject.application.validator.impl;

import com.javaproject.application.dto.SecurityConfigDto;
import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.RegisterUserRequest;
import com.javaproject.application.enums.AuthParameterEnum;
import com.javaproject.application.service.impl.AuthParameterService;
import com.javaproject.application.service.impl.SecurityPolicyService;
import com.javaproject.application.validator.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterUserValidator implements Validator {

    private final AuthParameterService authParameterService;
    private final SecurityPolicyService securityPolicyService;
    @Override
    public void validateRequest(BaseRequest baseRequest) {
        RegisterUserRequest registerUserRequest = (RegisterUserRequest) baseRequest;
        if (isBlank(registerUserRequest.getEmail()) && isBlank(registerUserRequest.getMobile())) {
            throw new IllegalArgumentException("Either email or mobile must be provided.");
        }

        if (Boolean.parseBoolean(authParameterService.getByPolicyName(AuthParameterEnum.IS_MFA_ENABLED.getParameterName()))
                && registerUserRequest.getMfaMethod() != null) {
            String mfaMethod = registerUserRequest.getMfaMethod();
            if (!mfaMethod.equalsIgnoreCase("SMS") && !mfaMethod.equalsIgnoreCase("EMAIL")) {
                throw new IllegalArgumentException("Invalid MFA method. Allowed values are 'SMS' or 'EMAIL'.");
            }
        }
        SecurityConfigDto securityConfigDto = securityPolicyService.getByConfigId("DEFAULT");
        if (registerUserRequest.getPassword().length() < securityConfigDto.getPasswordMinLength()) {
            throw new IllegalArgumentException("Password must be at least " + securityConfigDto.getPasswordMinLength() + " characters long.");
        }
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.trim().isEmpty();
    }
}
