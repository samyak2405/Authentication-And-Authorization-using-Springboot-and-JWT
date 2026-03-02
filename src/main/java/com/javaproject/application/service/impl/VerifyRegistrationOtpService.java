package com.javaproject.application.service.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.VerifyRegistrationOtpRequest;
import com.javaproject.application.dto.response.ApiResponse;
import com.javaproject.application.dto.response.VerifyRegistrationOtpResponse;
import com.javaproject.application.exception.custom.ProcessApiException;
import com.javaproject.application.model.OtpToken;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.OtpTokenRepository;
import com.javaproject.application.repository.UserRepository;
import com.javaproject.application.service.ProcessRequest;
import com.javaproject.application.util.PasswordUtility;
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
    private final OtpTokenRepository otpTokenRepository;

    @Value("${app.notifications.registration-otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.notifications.registration-otp.cooldown-minutes:15}")
    private int cooldownMinutes;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ApiResponse<VerifyRegistrationOtpResponse> processApiRequest(BaseRequest baseRequest) {
        VerifyRegistrationOtpRequest request = (VerifyRegistrationOtpRequest) baseRequest;

        User user = userRepository.getByEmail(request.getEmail())
                .orElseThrow(() -> new ProcessApiException("User not found", HttpStatus.NOT_FOUND));

        if (user.isActive()) {
            return successResponse(request, user, "Account already active.");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new ProcessApiException(
                    "Account is in cooldown until " + user.getLockedUntil(),
                    HttpStatus.FORBIDDEN
            );
        }

        OtpToken otpToken = otpTokenRepository
                .findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIssuedAtDesc(user, REGISTRATION_OTP_PURPOSE)
                .orElseThrow(() -> new ProcessApiException("OTP not found for verification", HttpStatus.BAD_REQUEST));

        if (otpToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ProcessApiException("OTP has expired", HttpStatus.BAD_REQUEST);
        }

        String providedOtpHash = PasswordUtility.hashToken(request.getOtp());
        if (!providedOtpHash.equals(otpToken.getTokenHash())) {
            int attempts = otpToken.getAttemptCount() + 1;
            otpToken.setAttemptCount(attempts);

            if (attempts >= maxAttempts) {
                OffsetDateTime now = OffsetDateTime.now();
                OffsetDateTime lockedUntil = now.plusMinutes(cooldownMinutes);
                otpToken.setConsumedAt(now);
                otpTokenRepository.save(otpToken);

                user.setLockedUntil(lockedUntil);
                user.setLockReason("REGISTRATION_OTP_ATTEMPTS_EXCEEDED");
                user.setUpdatedAt(now);
                userRepository.save(user);

                throw new ProcessApiException(
                        "Too many invalid OTP attempts. Account is in cooldown until " + lockedUntil,
                        HttpStatus.FORBIDDEN
                );
            }
            otpTokenRepository.save(otpToken);
            throw new ProcessApiException("Invalid OTP", HttpStatus.UNAUTHORIZED);
        }

        otpToken.setAttemptCount(otpToken.getAttemptCount() + 1);
        otpToken.setConsumedAt(OffsetDateTime.now());
        otpTokenRepository.save(otpToken);

        user.setActive(true);
        user.setLockedUntil(null);
        user.setLockReason(null);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Registration OTP verified successfully for user={}", user.getEmail());
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
                .active(user.isActive())
                .build());
        return response;
    }
}
