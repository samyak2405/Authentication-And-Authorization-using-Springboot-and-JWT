package com.javaproject.application.validator.factory;

import com.javaproject.application.validator.Validator;
import com.javaproject.application.validator.impl.LoginRequestValidator;
import com.javaproject.application.validator.impl.RegisterUserValidator;
import com.javaproject.application.validator.impl.ResendRegistrationOtpValidator;
import com.javaproject.application.validator.impl.VerifyRegistrationOtpValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidatorFactory {

    private final RegisterUserValidator registerUserValidator;
    private final LoginRequestValidator loginRequestValidator;
    private final VerifyRegistrationOtpValidator verifyRegistrationOtpValidator;
    private final ResendRegistrationOtpValidator resendRegistrationOtpValidator;

    public Validator getValidator(String validatorType){
       return switch (validatorType){
            case "REGISTER" -> registerUserValidator;
            case "LOGIN" -> loginRequestValidator;
           case "VERIFY_REGISTRATION_OTP" -> verifyRegistrationOtpValidator;
           case "RESEND_REGISTRATION_OTP" -> resendRegistrationOtpValidator;
           default -> throw new IllegalArgumentException("invalid validator type");
        };
    }
}
