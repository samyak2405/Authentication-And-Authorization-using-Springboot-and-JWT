package com.javaproject.application.service.impl;

import com.javaproject.application.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtpTokenCleanupJob {

    private final OtpTokenRepository otpTokenRepository;

    @Value("${app.notifications.registration-otp.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${app.notifications.registration-otp.cleanup.retention-hours:24}")
    private int cleanupRetentionHours;

    @Scheduled(fixedDelayString = "${app.notifications.registration-otp.cleanup.fixed-delay-ms:900000}")
    @Transactional
    public void cleanupExpiredAndConsumedTokens() {
        if (!cleanupEnabled) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        long deleted = otpTokenRepository.deleteByExpiresAtBeforeOrConsumedAtBefore(
                now,
                now.minusHours(cleanupRetentionHours)
        );
        if (deleted > 0) {
            log.info("Deleted {} stale OTP token rows", deleted);
        }
    }
}
