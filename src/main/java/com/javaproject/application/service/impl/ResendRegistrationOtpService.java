package com.javaproject.application.service.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.ResendRegistrationOtpRequest;
import com.javaproject.application.dto.response.ApiResponse;
import com.javaproject.application.dto.response.ResendRegistrationOtpResponse;
import com.javaproject.application.enums.NotificationChannel;
import com.javaproject.application.exception.custom.ProcessApiException;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.UserRepository;
import com.javaproject.application.service.ProcessRequest;
import com.javaproject.application.service.otp.OtpStore;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final RegistrationOtpService registrationOtpService;
    private final OtpStore otpStore;
    private final MeterRegistry meterRegistry;

    @Value("${app.notifications.registration-otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ApiResponse<ResendRegistrationOtpResponse> processApiRequest(BaseRequest baseRequest) {
        ResendRegistrationOtpRequest request = (ResendRegistrationOtpRequest) baseRequest;

        User user = findUser(request.getEmail(), request.getMobile());
        String requestedMobile = normalize(request.getMobile());
        if (requestedMobile != null && !requestedMobile.equals(user.getMobile())) {
            user.setMobile(requestedMobile);
            user.setUpdatedAt(OffsetDateTime.now());
            user = userRepository.save(user);
        }

        if (user.isActive()) {
            throw new ProcessApiException("Account is already active", HttpStatus.CONFLICT);
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new ProcessApiException(
                    "Account is in cooldown until " + user.getLockedUntil(),
                    HttpStatus.FORBIDDEN
            );
        }

        OffsetDateTime now = OffsetDateTime.now();
        OtpStore.OtpResendCheckResult resendCheck = otpStore.checkResendAllowed(
                OtpStore.OtpResendCheckRequest.builder()
                        .user(user)
                        .purpose(REGISTRATION_OTP_PURPOSE)
                        .resendCooldownSeconds(resendCooldownSeconds)
                        .now(now)
                        .build()
        );
        if (!resendCheck.isResendAllowed()) {
            Counter.builder("auth.otp.resend.denied")
                    .tag("reason", "cooldown")
                    .register(meterRegistry)
                    .increment();
            throw new ProcessApiException(
                    "Please wait before requesting OTP again. Retry in " + resendCheck.getWaitSeconds() + " seconds.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        NotificationChannel preferredChannel = resolvePreferredChannel(request.getOtpChannel());
        RegistrationOtpService.OtpIssueResult issueResult =
                registrationOtpService.issueOtp(user, request, "auth-resend-otp", true, preferredChannel);

        log.info("Resent registration OTP for user={}", user.getEmail());
        Counter.builder("auth.otp.resend.success").register(meterRegistry).increment();

        ApiResponse<ResendRegistrationOtpResponse> response = new ApiResponse<>();
        response.setRequestId(request.getRequestId());
        response.setSuccess(true);
        response.setResponseCode(String.valueOf(HttpStatus.OK.value()));
        response.setResponseMessage("OTP resent successfully.");
        response.setTimestamp(OffsetDateTime.now());
        response.setData(ResendRegistrationOtpResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .otpExpiresAt(issueResult.getExpiresAt())
                .build());
        return response;
    }

    private User findUser(String email, String mobile) {
        if (email != null && !email.isBlank()) {
            return userRepository.getByEmail(email.trim())
                    .orElseThrow(() -> new ProcessApiException("User not found", HttpStatus.NOT_FOUND));
        }
        if (mobile != null && !mobile.isBlank()) {
            return userRepository.getByMobile(mobile.trim())
                    .orElseThrow(() -> new ProcessApiException("User not found", HttpStatus.NOT_FOUND));
        }
        throw new ProcessApiException("Either email or mobile must be provided", HttpStatus.BAD_REQUEST);
    }

    private NotificationChannel resolvePreferredChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return null;
        }
        try {
            return NotificationChannel.valueOf(channel.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ProcessApiException("otpChannel must be either EMAIL or SMS", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
