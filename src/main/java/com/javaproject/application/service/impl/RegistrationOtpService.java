package com.javaproject.application.service.impl;

import com.javaproject.application.dto.notification.NotificationEventMessage;
import com.javaproject.application.dto.notification.RegistrationOtpPayload;
import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.enums.NotificationChannel;
import com.javaproject.application.enums.NotificationEventType;
import com.javaproject.application.model.User;
import com.javaproject.application.service.otp.OtpStore;
import com.javaproject.application.util.PasswordUtility;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationOtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String REGISTRATION_OTP_PURPOSE = "OTP";

    private final OtpStore otpStore;
    private final NotificationEventPublisher notificationEventPublisher;
    private final MeterRegistry meterRegistry;

    @Value("${app.notifications.registration-otp.expiry-minutes:10}")
    private int registrationOtpExpiryMinutes;

    public OtpIssueResult issueOtp(
            User user,
            BaseRequest request,
            String idempotencyKeyPrefix,
            boolean invalidateExistingTokens,
            NotificationChannel preferredChannel
    ) {
        String otp = generateOtp();
        OffsetDateTime issuedAt = OffsetDateTime.now();
        OffsetDateTime expiresAt = issuedAt.plusMinutes(registrationOtpExpiryMinutes);
        NotificationChannel deliveryChannel = resolveDeliveryChannel(user, preferredChannel);

        OtpStore.OtpIssueResult otpIssueResult = otpStore.issue(OtpStore.OtpIssueRequest.builder()
                .user(user)
                .purpose(REGISTRATION_OTP_PURPOSE)
                .tokenHash(PasswordUtility.hashToken(otp))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .resendCooldownSeconds(0)
                .ipAddress(request.getIpAddress())
                .deliveryChannel(deliveryChannel.name())
                .invalidateExistingTokens(invalidateExistingTokens)
                .build());

        NotificationEventMessage message = NotificationEventMessage.builder()
                .notificationId(UUID.randomUUID().toString())
                .userId(user.getId().toString())
                .channel(deliveryChannel)
                .eventType(NotificationEventType.REGISTRATION_OTP)
                .idempotencyKey(idempotencyKeyPrefix + "-" + request.getRequestId())
                .attempt(1)
                .createdAt(Instant.now().toString())
                .payload(RegistrationOtpPayload.builder()
                        .email(user.getEmail())
                        .mobile(user.getMobile())
                        .otp(otp)
                        .expiryMinutes(registrationOtpExpiryMinutes)
                        .build())
                .build();
        notificationEventPublisher.publishRegistrationOtpEvent(message, request.getRequestId());
        Counter.builder("auth.otp.issue.success")
                .tag("purpose", REGISTRATION_OTP_PURPOSE)
                .tag("channel", deliveryChannel.name())
                .register(meterRegistry)
                .increment();

        return OtpIssueResult.builder()
                .issuedAt(otpIssueResult.getIssuedAt())
                .expiresAt(otpIssueResult.getExpiresAt())
                .build();
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private NotificationChannel resolveDeliveryChannel(User user, NotificationChannel preferredChannel) {
        if (preferredChannel != null) {
            if (preferredChannel == NotificationChannel.SMS) {
                if (user.getMobile() == null || user.getMobile().isBlank()) {
                    throw new IllegalStateException("Unable to issue OTP on SMS: mobile is missing.");
                }
                return NotificationChannel.SMS;
            }
            if (preferredChannel == NotificationChannel.EMAIL) {
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    throw new IllegalStateException("Unable to issue OTP on EMAIL: email is missing.");
                }
                return NotificationChannel.EMAIL;
            }
        }
        if (user.getMobile() != null && !user.getMobile().isBlank()) {
            return NotificationChannel.SMS;
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return NotificationChannel.EMAIL;
        }
        throw new IllegalStateException("Unable to issue OTP: both email and mobile are empty.");
    }

    @Getter
    @Builder
    public static class OtpIssueResult {
        private OffsetDateTime issuedAt;
        private OffsetDateTime expiresAt;
    }
}
