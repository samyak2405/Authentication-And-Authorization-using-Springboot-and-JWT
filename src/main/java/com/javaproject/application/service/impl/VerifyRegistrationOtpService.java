package com.javaproject.application.service.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.VerifyRegistrationOtpRequest;
import com.javaproject.application.dto.response.ApiResponse;
import com.javaproject.application.dto.response.VerifyRegistrationOtpResponse;
import com.javaproject.application.exception.custom.ProcessApiException;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.UserRepository;
import com.javaproject.application.service.ProcessRequest;
import com.javaproject.application.service.otp.OtpStore;
import com.javaproject.application.util.PasswordUtility;
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
public class VerifyRegistrationOtpService implements ProcessRequest {

    private static final String REGISTRATION_OTP_PURPOSE = "OTP";

    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final MeterRegistry meterRegistry;

    @Value("${app.notifications.registration-otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.notifications.registration-otp.cooldown-minutes:15}")
    private int cooldownMinutes;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ApiResponse<VerifyRegistrationOtpResponse> processApiRequest(BaseRequest baseRequest) {
        VerifyRegistrationOtpRequest request = (VerifyRegistrationOtpRequest) baseRequest;

        User user = findUser(request.getEmail(), request.getMobile());

        if (user.isActive()) {
            return successResponse(request, user, "Account already active.");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new ProcessApiException(
                    "Account is in cooldown until " + user.getLockedUntil(),
                    HttpStatus.FORBIDDEN
            );
        }

        OffsetDateTime now = OffsetDateTime.now();
        OtpStore.OtpVerifyResult verifyResult = otpStore.verify(OtpStore.OtpVerifyRequest.builder()
                .user(user)
                .purpose(REGISTRATION_OTP_PURPOSE)
                .providedTokenHash(PasswordUtility.hashToken(request.getOtp()))
                .maxAttempts(maxAttempts)
                .now(now)
                .build());

        if (verifyResult.getStatus() == OtpStore.OtpVerifyStatus.NOT_FOUND) {
            Counter.builder("auth.otp.verify.failure").tag("reason", "not_found").register(meterRegistry).increment();
            throw new ProcessApiException("OTP not found for verification", HttpStatus.BAD_REQUEST);
        }
        if (verifyResult.getStatus() == OtpStore.OtpVerifyStatus.EXPIRED) {
            Counter.builder("auth.otp.verify.failure").tag("reason", "expired").register(meterRegistry).increment();
            throw new ProcessApiException("OTP has expired", HttpStatus.BAD_REQUEST);
        }
        if (verifyResult.getStatus() == OtpStore.OtpVerifyStatus.INVALID) {
            Counter.builder("auth.otp.verify.failure").tag("reason", "invalid").register(meterRegistry).increment();
            throw new ProcessApiException("Invalid OTP", HttpStatus.UNAUTHORIZED);
        }
        if (verifyResult.getStatus() == OtpStore.OtpVerifyStatus.ATTEMPTS_EXCEEDED) {
            OffsetDateTime lockedUntil = now.plusMinutes(cooldownMinutes);
            user.setLockedUntil(lockedUntil);
            user.setLockReason("REGISTRATION_OTP_ATTEMPTS_EXCEEDED");
            user.setUpdatedAt(now);
            userRepository.save(user);
            Counter.builder("auth.otp.verify.failure").tag("reason", "attempts_exceeded").register(meterRegistry).increment();
            throw new ProcessApiException(
                    "Too many invalid OTP attempts. Account is in cooldown until " + lockedUntil,
                    HttpStatus.FORBIDDEN
            );
        }

        user.setActive(true);
        user.setLockedUntil(null);
        user.setLockReason(null);
        user.setUpdatedAt(now);
        userRepository.save(user);

        log.info("Registration OTP verified successfully for user={}", user.getEmail());
        Counter.builder("auth.otp.verify.success").register(meterRegistry).increment();
        return successResponse(request, user, "OTP verified. Account activated.");
    }

    private ApiResponse<VerifyRegistrationOtpResponse> successResponse(
            VerifyRegistrationOtpRequest request,
            User user,
            String message
    ) {
        ApiResponse<VerifyRegistrationOtpResponse> response = new ApiResponse<>();
        response.setRequestId(request.getRequestId());
        response.setSuccess(true);
        response.setResponseCode(String.valueOf(HttpStatus.OK.value()));
        response.setResponseMessage(message);
        response.setTimestamp(OffsetDateTime.now());
        response.setData(VerifyRegistrationOtpResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .active(user.isActive())
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
}
