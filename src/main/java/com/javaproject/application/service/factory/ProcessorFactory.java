package com.javaproject.application.service.factory;

import com.javaproject.application.service.ProcessRequest;
import com.javaproject.application.service.impl.LoginService;
import com.javaproject.application.service.impl.RegisterUserService;
import com.javaproject.application.service.impl.ResendRegistrationOtpService;
import com.javaproject.application.service.impl.VerifyRegistrationOtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessorFactory {
    private final RegisterUserService registerUserService;
    private final LoginService loginService;
    private final VerifyRegistrationOtpService verifyRegistrationOtpService;
    private final ResendRegistrationOtpService resendRegistrationOtpService;


    public ProcessRequest getProcessor(String processorType) {
        return switch (processorType) {
            case "LOGIN" -> loginService;
            case "REGISTER" -> registerUserService;
            case "VERIFY_REGISTRATION_OTP" -> verifyRegistrationOtpService;
            case "RESEND_REGISTRATION_OTP" -> resendRegistrationOtpService;
            default -> throw new IllegalArgumentException("Invalid processor type");
        };
    }
}
