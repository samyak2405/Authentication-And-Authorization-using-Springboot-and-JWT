package com.javaproject.application.security.authentication;

import com.javaproject.application.dto.request.LoginUserRequest;
import com.javaproject.application.exception.custom.ProcessApiException;
import com.javaproject.application.exception.custom.UserNotFoundException;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.UserRepository;
import com.javaproject.application.service.otp.OtpStore;
import com.javaproject.application.util.PasswordUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class MobileOtpLoginCredentialStrategy implements LoginCredentialStrategy {

    private static final String OTP_PURPOSE = "OTP";

    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final UserLoginSecurityService userLoginSecurityService;

    @Value("${app.notifications.login-otp.max-attempts:5}")
    private int loginOtpMaxAttempts;

    @Value("${app.notifications.login-otp.cooldown-minutes:15}")
    private int loginOtpCooldownMinutes;

    @Override
    public String getName() {
        return "MOBILE_OTP";
    }

    @Override
    public boolean supports(LoginUserRequest loginRequest) {
        return hasText(loginRequest.getMobile()) && hasText(loginRequest.getOtp());
    }

    @Override
    public User authenticate(LoginUserRequest loginRequest) {
        User user = userRepository.getByMobile(loginRequest.getMobile().trim())
                .orElseThrow(() -> new UserNotFoundException("Invalid mobile or otp"));
        userLoginSecurityService.validateAccountState(user);

        OffsetDateTime now = OffsetDateTime.now();
        OtpStore.OtpVerifyResult verifyResult = otpStore.verify(OtpStore.OtpVerifyRequest.builder()
                .user(user)
                .purpose(OTP_PURPOSE)
                .providedTokenHash(PasswordUtility.hashToken(loginRequest.getOtp()))
                .maxAttempts(loginOtpMaxAttempts)
                .now(now)
                .build());

        if (verifyResult.getStatus() == OtpStore.OtpVerifyStatus.NOT_FOUND) {
            userLoginSecurityService.handleFailedLogin(user, loginRequest.getMobile());
            throw new ProcessApiException("Invalid mobile or otp", HttpStatus.UNAUTHORIZED);
        }
        if (verifyResult.getStatus() == OtpStore.OtpVerifyStatus.EXPIRED) {
            userLoginSecurityService.handleFailedLogin(user, loginRequest.getMobile());
            throw new ProcessApiException("OTP has expired", HttpStatus.UNAUTHORIZED);
        }
        if (verifyResult.getStatus() == OtpStore.OtpVerifyStatus.INVALID) {
            userLoginSecurityService.handleFailedLogin(user, loginRequest.getMobile());
            throw new ProcessApiException("Invalid mobile or otp", HttpStatus.UNAUTHORIZED);
        }
        if (verifyResult.getStatus() == OtpStore.OtpVerifyStatus.ATTEMPTS_EXCEEDED) {
            userLoginSecurityService.handleFailedLogin(user, loginRequest.getMobile());
            user.setLockedUntil(now.plusMinutes(loginOtpCooldownMinutes));
            user.setLockReason("LOGIN_OTP_ATTEMPTS_EXCEEDED");
            userRepository.save(user);
            throw new ProcessApiException(
                    "Too many invalid OTP attempts. Account is locked until " + user.getLockedUntil(),
                    HttpStatus.FORBIDDEN
            );
        }

        return user;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
