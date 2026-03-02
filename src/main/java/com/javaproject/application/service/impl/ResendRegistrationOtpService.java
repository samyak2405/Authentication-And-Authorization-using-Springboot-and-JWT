package com.javaproject.application.service.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.ResendRegistrationOtpRequest;
import com.javaproject.application.dto.response.ApiResponse;
import com.javaproject.application.dto.response.ResendRegistrationOtpResponse;
import com.javaproject.application.exception.custom.ProcessApiException;
import com.javaproject.application.model.OtpToken;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.OtpTokenRepository;
import com.javaproject.application.repository.UserRepository;
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
@Service
@RequiredArgsConstructor
public class ResendRegistrationOtpService implements ProcessRequest {

    private static final String REGISTRATION_OTP_PURPOSE = "OTP";

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final RegistrationOtpService registrationOtpService;

    @Value("${app.notifications.registration-otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ApiResponse<ResendRegistrationOtpResponse> processApiRequest(BaseRequest baseRequest) {
        ResendRegistrationOtpRequest request = (ResendRegistrationOtpRequest) baseRequest;

        User user = userRepository.getByEmail(request.getEmail())
                .orElseThrow(() -> new ProcessApiException("User not found", HttpStatus.NOT_FOUND));

        if (user.isActive()) {
            throw new ProcessApiException("Account is already active", HttpStatus.CONFLICT);
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new ProcessApiException(
                    "Account is in cooldown until " + user.getLockedUntil(),
                    HttpStatus.FORBIDDEN
            );
        }

        OtpToken latestUnconsumed = otpTokenRepository
                .findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIssuedAtDesc(user, REGISTRATION_OTP_PURPOSE)
                .orElse(null);

        OffsetDateTime now = OffsetDateTime.now();
        if (latestUnconsumed != null && latestUnconsumed.getIssuedAt().plusSeconds(resendCooldownSeconds).isAfter(now)) {
            throw new ProcessApiException(
                    "Please wait before requesting OTP again.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        RegistrationOtpService.OtpIssueResult issueResult =
                registrationOtpService.issueOtp(user, request, "auth-resend-otp", true);

        log.info("Resent registration OTP for user={}", user.getEmail());

        ApiResponse<ResendRegistrationOtpResponse> response = new ApiResponse<>();
        response.setRequestId(request.getRequestId());
        response.setSuccess(true);
        response.setResponseCode(String.valueOf(HttpStatus.OK.value()));
        response.setResponseMessage("OTP resent successfully.");
        response.setTimestamp(OffsetDateTime.now());
        response.setData(ResendRegistrationOtpResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .otpExpiresAt(issueResult.getExpiresAt())
                .build());
        return response;
    }
}
