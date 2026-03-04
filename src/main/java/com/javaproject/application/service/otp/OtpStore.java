package com.javaproject.application.service.otp;

import com.javaproject.application.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

public interface OtpStore {

    OtpIssueResult issue(OtpIssueRequest request);

    OtpResendCheckResult checkResendAllowed(OtpResendCheckRequest request);

    OtpVerifyResult verify(OtpVerifyRequest request);

    void invalidateActive(User user, String purpose, OffsetDateTime now);

    @Getter
    @Builder
    class OtpIssueRequest {
        private User user;
        private String purpose;
        private String tokenHash;
        private OffsetDateTime issuedAt;
        private OffsetDateTime expiresAt;
        private int resendCooldownSeconds;
        private String ipAddress;
        private String deliveryChannel;
        private boolean invalidateExistingTokens;
    }

    @Getter
    @Builder
    class OtpIssueResult {
        private OffsetDateTime issuedAt;
        private OffsetDateTime expiresAt;
    }

    @Getter
    @Builder
    class OtpResendCheckRequest {
        private User user;
        private String purpose;
        private int resendCooldownSeconds;
        private OffsetDateTime now;
    }

    @Getter
    @Builder
    class OtpResendCheckResult {
        private boolean resendAllowed;
        private long waitSeconds;
    }

    @Getter
    @Builder
    class OtpVerifyRequest {
        private User user;
        private String purpose;
        private String providedTokenHash;
        private int maxAttempts;
        private OffsetDateTime now;
    }

    @Getter
    @Builder
    class OtpVerifyResult {
        private OtpVerifyStatus status;
        private int attempts;
    }

    enum OtpVerifyStatus {
        VERIFIED,
        INVALID,
        NOT_FOUND,
        EXPIRED,
        ATTEMPTS_EXCEEDED
    }
}
