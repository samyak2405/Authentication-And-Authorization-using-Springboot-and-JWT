package com.javaproject.application.service.otp;

import com.javaproject.application.model.OtpToken;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service("dbOtpStore")
@RequiredArgsConstructor
public class DbOtpStore implements OtpStore {

    private final OtpTokenRepository otpTokenRepository;

    @Override
    @Transactional
    public OtpIssueResult issue(OtpIssueRequest request) {
        if (request.isInvalidateExistingTokens()) {
            invalidateActive(request.getUser(), request.getPurpose(), request.getIssuedAt());
        }

        OtpToken otpToken = OtpToken.builder()
                .user(request.getUser())
                .tokenHash(request.getTokenHash())
                .purpose(request.getPurpose())
                .issuedAt(request.getIssuedAt())
                .expiresAt(request.getExpiresAt())
                .consumedAt(null)
                .attemptCount(0)
                .ipAddress(request.getIpAddress())
                .deliveryChannel(request.getDeliveryChannel())
                .build();
        otpTokenRepository.save(otpToken);

        return OtpIssueResult.builder()
                .issuedAt(request.getIssuedAt())
                .expiresAt(request.getExpiresAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OtpResendCheckResult checkResendAllowed(OtpResendCheckRequest request) {
        OtpToken latestUnconsumed = otpTokenRepository
                .findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIssuedAtDesc(request.getUser(), request.getPurpose())
                .orElse(null);

        if (latestUnconsumed == null) {
            return OtpResendCheckResult.builder().resendAllowed(true).waitSeconds(0).build();
        }

        OffsetDateTime resendAllowedAt = latestUnconsumed.getIssuedAt().plusSeconds(request.getResendCooldownSeconds());
        if (resendAllowedAt.isAfter(request.getNow())) {
            long waitSeconds = Math.max(1, resendAllowedAt.toEpochSecond() - request.getNow().toEpochSecond());
            return OtpResendCheckResult.builder().resendAllowed(false).waitSeconds(waitSeconds).build();
        }

        return OtpResendCheckResult.builder().resendAllowed(true).waitSeconds(0).build();
    }

    @Override
    @Transactional
    public OtpVerifyResult verify(OtpVerifyRequest request) {
        OtpToken otpToken = otpTokenRepository
                .findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIssuedAtDesc(request.getUser(), request.getPurpose())
                .orElse(null);

        if (otpToken == null) {
            return OtpVerifyResult.builder()
                    .status(OtpVerifyStatus.NOT_FOUND)
                    .attempts(0)
                    .build();
        }

        if (otpToken.getExpiresAt().isBefore(request.getNow())) {
            return OtpVerifyResult.builder()
                    .status(OtpVerifyStatus.EXPIRED)
                    .attempts(otpToken.getAttemptCount())
                    .build();
        }

        int attempts = otpToken.getAttemptCount() + 1;
        otpToken.setAttemptCount(attempts);
        if (request.getProvidedTokenHash().equals(otpToken.getTokenHash())) {
            otpToken.setConsumedAt(request.getNow());
            otpTokenRepository.save(otpToken);
            return OtpVerifyResult.builder()
                    .status(OtpVerifyStatus.VERIFIED)
                    .attempts(attempts)
                    .build();
        }

        if (attempts >= request.getMaxAttempts()) {
            otpToken.setConsumedAt(request.getNow());
            otpTokenRepository.save(otpToken);
            return OtpVerifyResult.builder()
                    .status(OtpVerifyStatus.ATTEMPTS_EXCEEDED)
                    .attempts(attempts)
                    .build();
        }

        otpTokenRepository.save(otpToken);
        return OtpVerifyResult.builder()
                .status(OtpVerifyStatus.INVALID)
                .attempts(attempts)
                .build();
    }

    @Override
    @Transactional
    public void invalidateActive(User user, String purpose, OffsetDateTime now) {
        List<OtpToken> activeTokens = otpTokenRepository.findByUserAndPurposeAndConsumedAtIsNull(user, purpose);
        for (OtpToken token : activeTokens) {
            token.setConsumedAt(now);
            otpTokenRepository.save(token);
        }
    }
}
