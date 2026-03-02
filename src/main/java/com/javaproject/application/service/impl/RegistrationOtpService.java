package com.javaproject.application.service.impl;

import com.javaproject.application.dto.notification.NotificationEventMessage;
import com.javaproject.application.dto.notification.RegistrationOtpPayload;
import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.enums.NotificationChannel;
import com.javaproject.application.enums.NotificationEventType;
import com.javaproject.application.model.OtpToken;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.OtpTokenRepository;
import com.javaproject.application.util.PasswordUtility;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationOtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String REGISTRATION_OTP_PURPOSE = "OTP";

    private final OtpTokenRepository otpTokenRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Value("${app.notifications.registration-otp.expiry-minutes:10}")
    private int registrationOtpExpiryMinutes;

    public OtpIssueResult issueOtp(
            User user,
            BaseRequest request,
            String idempotencyKeyPrefix,
            boolean invalidateExistingTokens
    ) {
        if (invalidateExistingTokens) {
            invalidateUnconsumedTokens(user);
        }

        String otp = generateUniqueOtp();
        OffsetDateTime issuedAt = OffsetDateTime.now();
        OffsetDateTime expiresAt = issuedAt.plusMinutes(registrationOtpExpiryMinutes);

        OtpToken otpToken = OtpToken.builder()
                .user(user)
                .tokenHash(PasswordUtility.hashToken(otp))
                .purpose(REGISTRATION_OTP_PURPOSE)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .consumedAt(null)
                .attemptCount(0)
                .ipAddress(request.getIpAddress())
                .deliveryChannel(NotificationChannel.EMAIL.name())
                .build();
        otpTokenRepository.save(otpToken);

        NotificationEventMessage message = NotificationEventMessage.builder()
                .notificationId(UUID.randomUUID().toString())
                .userId(user.getId().toString())
                .channel(NotificationChannel.EMAIL)
                .eventType(NotificationEventType.REGISTRATION_OTP)
                .idempotencyKey(idempotencyKeyPrefix + "-" + request.getRequestId())
                .attempt(1)
                .createdAt(Instant.now().toString())
                .payload(RegistrationOtpPayload.builder()
                        .email(user.getEmail())
                        .otp(otp)
                        .expiryMinutes(registrationOtpExpiryMinutes)
                        .build())
                .build();
        notificationEventPublisher.publishRegistrationOtpEvent(message, request.getRequestId());

        return OtpIssueResult.builder()
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    private void invalidateUnconsumedTokens(User user) {
        List<OtpToken> activeTokens = otpTokenRepository.findByUserAndPurposeAndConsumedAtIsNull(
                user, REGISTRATION_OTP_PURPOSE
        );
        OffsetDateTime now = OffsetDateTime.now();
        for (OtpToken token : activeTokens) {
            token.setConsumedAt(now);
            otpTokenRepository.save(token);
        }
    }

    private String generateUniqueOtp() {
        for (int i = 0; i < 10; i++) {
            String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
            String otpHash = PasswordUtility.hashToken(otp);
            if (!otpTokenRepository.existsByTokenHash(otpHash)) {
                return otp;
            }
        }
        throw new IllegalStateException("Unable to generate unique OTP. Please retry.");
    }

    @Getter
    @Builder
    public static class OtpIssueResult {
        private OffsetDateTime issuedAt;
        private OffsetDateTime expiresAt;
    }
}
