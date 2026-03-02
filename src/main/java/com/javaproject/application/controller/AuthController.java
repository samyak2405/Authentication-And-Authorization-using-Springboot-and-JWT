package com.javaproject.application.controller;

import com.javaproject.application.dto.request.LoginUserRequest;
import com.javaproject.application.dto.request.RegisterUserRequest;
import com.javaproject.application.dto.request.ResendRegistrationOtpRequest;
import com.javaproject.application.dto.request.VerifyRegistrationOtpRequest;
import com.javaproject.application.dto.response.ApiResponse;
import com.javaproject.application.dto.response.LoginUserResponse;
import com.javaproject.application.enums.ApiTypeEnum;
import com.javaproject.application.security.authentication.AuthenticationResult;
import com.javaproject.application.service.ProcessRequest;
import com.javaproject.application.service.factory.ProcessorFactory;
import com.javaproject.application.service.impl.LoginService;
import com.javaproject.application.util.CookieUtil;
import com.javaproject.application.validator.Validator;
import com.javaproject.application.validator.factory.ValidatorFactory;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;


@Slf4j
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {

    private final ValidatorFactory validatorFactory;
    private final ProcessorFactory processorFactory;
    private final LoginService loginService;
    private final CookieUtil cookieUtil;

    @PostMapping("/v1/register")
    public ResponseEntity<ApiResponse<?>> registerUser(@Valid @RequestBody RegisterUserRequest registerUserRequest) {
        log.info("Received registration request for user: {}", registerUserRequest.getEmail());
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.REGISTER.getApiType());
        ProcessRequest processor = processorFactory.getProcessor(ApiTypeEnum.REGISTER.getApiType());
        log.info("Validating registration request for user: {}", registerUserRequest.getEmail());
        validator.validateRequest(registerUserRequest);
        log.info("Processing registration request for user: {}", registerUserRequest.getEmail());
        ApiResponse<?> apiResponse = processor.processApiRequest(registerUserRequest);
        log.info("Processing completed for registration request for user: {}", apiResponse.toString());
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/login")
    public ResponseEntity<ApiResponse<LoginUserResponse>> loginUser(
            @Valid @RequestBody LoginUserRequest loginUserRequest,
            HttpServletResponse response
    ) {
        log.info("Received login request for user: {}", loginUserRequest.getEmail());
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.LOGIN.getApiType());
        log.info("Validating login request for user: {}", loginUserRequest.getEmail());
        validator.validateRequest(loginUserRequest);

        // Authenticate and get tokens
        AuthenticationResult authResult = loginService.authenticate(loginUserRequest);

        // Set tokens in HttpOnly cookies (NOT in response body)
        cookieUtil.addTokenCookies(response, authResult.getAccessToken(), authResult.getRefreshToken());

        // Build response body without tokens
        LoginUserResponse responseData = LoginUserResponse.builder()
                .userId(authResult.getUserId())
                .email(authResult.getEmail())
                .roles(authResult.getRoles())
                .accessTokenExpiresAt(authResult.getAccessTokenExpiresAt())
                .build();

        ApiResponse<LoginUserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setRequestId(loginUserRequest.getRequestId());
        apiResponse.setSuccess(true);
        apiResponse.setResponseCode(String.valueOf(HttpStatus.OK.value()));
        apiResponse.setResponseMessage("Login successful");
        apiResponse.setTimestamp(OffsetDateTime.now());
        apiResponse.setData(responseData);

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/verify-registration-otp")
    public ResponseEntity<ApiResponse<?>> verifyRegistrationOtp(
            @Valid @RequestBody VerifyRegistrationOtpRequest verifyRegistrationOtpRequest
    ) {
        log.info("Received registration OTP verification request for user: {}", verifyRegistrationOtpRequest.getEmail());
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.VERIFY_REGISTRATION_OTP.getApiType());
        ProcessRequest processor = processorFactory.getProcessor(ApiTypeEnum.VERIFY_REGISTRATION_OTP.getApiType());
        validator.validateRequest(verifyRegistrationOtpRequest);
        ApiResponse<?> apiResponse = processor.processApiRequest(verifyRegistrationOtpRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/resend-registration-otp")
    public ResponseEntity<ApiResponse<?>> resendRegistrationOtp(
            @Valid @RequestBody ResendRegistrationOtpRequest resendRegistrationOtpRequest
    ) {
        log.info("Received resend registration OTP request for user: {}", resendRegistrationOtpRequest.getEmail());
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.RESEND_REGISTRATION_OTP.getApiType());
        ProcessRequest processor = processorFactory.getProcessor(ApiTypeEnum.RESEND_REGISTRATION_OTP.getApiType());
        validator.validateRequest(resendRegistrationOtpRequest);
        ApiResponse<?> apiResponse = processor.processApiRequest(resendRegistrationOtpRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/logout")
    public ResponseEntity<ApiResponse<Object>> logoutUser(HttpServletResponse response) {
        cookieUtil.clearTokenCookies(response);

        ApiResponse<Object> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(true);
        apiResponse.setResponseCode(String.valueOf(HttpStatus.OK.value()));
        apiResponse.setResponseMessage("Logout successful");
        apiResponse.setTimestamp(OffsetDateTime.now());

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/forgot-password")
    public String forgotPassword() {
        return "success";
    }

    @PostMapping("/v1/reset-password")
    public String resetPassword() {
        return "success";
    }
}
