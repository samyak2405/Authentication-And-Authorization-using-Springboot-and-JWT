package com.javaproject.application.service.impl;

import com.javaproject.application.dto.SecurityConfigDto;
import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.RegisterUserRequest;
import com.javaproject.application.dto.response.ApiResponse;
import com.javaproject.application.dto.response.RegisterUserResponse;
import com.javaproject.application.exception.custom.DBException;
import com.javaproject.application.exception.custom.UserAlreadyExistsException;
import com.javaproject.application.mapper.Mapper;
import com.javaproject.application.model.PasswordHistory;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.PasswordHistoryRepository;
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
public class RegisterUserService implements ProcessRequest {

    private final UserRepository userRepository;
    private final SecurityPolicyService securityPolicyService;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final RegistrationOtpService registrationOtpService;

    @Value("${app.security.policy.enable:DEFAULT}")
    private String enableSecurityPolicy;

    @Value("${app.security.password.algo:BCRYPT}")
    private String passwordAlgo;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ApiResponse<RegisterUserResponse> processApiRequest(BaseRequest baseRequest) {

        log.info("RegisterUserService: Processing registration request for user: {}", baseRequest.toString());
        ApiResponse<RegisterUserResponse> apiResponse = new ApiResponse<>();
        RegisterUserRequest registerUserRequest = (RegisterUserRequest) baseRequest;
        try {
            if (isNotBlank(registerUserRequest.getEmail())) {
                var existingByEmail = userRepository.getByEmail(registerUserRequest.getEmail().trim());
                if (existingByEmail.isPresent()) {
                    User existing = existingByEmail.get();
                    if (!existing.isActive()) {
                        apiResponse.setSuccess(false);
                        apiResponse.setResponseCode("VERIFICATION_PENDING");
                        apiResponse.setResponseMessage("Already signed up. Please verify your account.");
                        apiResponse.setRequestId(baseRequest.getRequestId());
                        apiResponse.setTimestamp(OffsetDateTime.now());
                        apiResponse.setData(RegisterUserResponse.builder()
                                .userId(existing.getId())
                                .email(existing.getEmail())
                                .mobile(existing.getMobile())
                                .build());
                        return apiResponse;
                    }
                    throw new UserAlreadyExistsException("A user with the provided email already exists.");
                }
            }

            if (isNotBlank(registerUserRequest.getMobile())) {
                var existingByMobile = userRepository.getByMobile(registerUserRequest.getMobile().trim());
                if (existingByMobile.isPresent()) {
                    User existing = existingByMobile.get();
                    if (!existing.isActive()) {
                        apiResponse.setSuccess(false);
                        apiResponse.setResponseCode("VERIFICATION_PENDING");
                        apiResponse.setResponseMessage("Already signed up. Please verify your account.");
                        apiResponse.setRequestId(baseRequest.getRequestId());
                        apiResponse.setTimestamp(OffsetDateTime.now());
                        apiResponse.setData(RegisterUserResponse.builder()
                                .userId(existing.getId())
                                .email(existing.getEmail())
                                .mobile(existing.getMobile())
                                .build());
                        return apiResponse;
                    }
                    throw new UserAlreadyExistsException("A user with the provided mobile already exists.");
                }
            }

            SecurityConfigDto securityConfigDto = securityPolicyService.getByConfigId(enableSecurityPolicy);
            User newUser = setUser(registerUserRequest, securityConfigDto);
            User savedUser = userRepository.save(newUser);
            PasswordHistory passwordHistory = setPasswordHistory(newUser);
            passwordHistoryRepository.save(passwordHistory);
            publishRegistrationOtpNotification(savedUser, baseRequest);

            apiResponse.setSuccess(true);
            apiResponse.setResponseMessage("User registered successfully.");
            apiResponse.setRequestId(baseRequest.getRequestId());
            apiResponse.setTimestamp(OffsetDateTime.now());
            apiResponse.setResponseCode(HttpStatus.OK.toString());
            apiResponse.setData(RegisterUserResponse.builder()
                    .userId(savedUser.getId())
                    .email(savedUser.getEmail())
                    .mobile(savedUser.getMobile())
                    .build());
        } catch (UserAlreadyExistsException e) {
            log.error("User already exists: {}", e.getMessage());
            apiResponse.setSuccess(false);
            apiResponse.setResponseMessage(e.getMessage());
            apiResponse.setRequestId(baseRequest.getRequestId());
            apiResponse.setTimestamp(OffsetDateTime.now());
            apiResponse.setResponseCode(HttpStatus.BAD_REQUEST.toString());
            return apiResponse;
        } catch (DBException dbe) {
            log.error("Database error occurred while processing registration request: {}", dbe.getMessage());
            apiResponse.setSuccess(false);
            apiResponse.setResponseMessage("A database error occurred while processing the registration request.");
            apiResponse.setRequestId(baseRequest.getRequestId());
            apiResponse.setTimestamp(OffsetDateTime.now());
            apiResponse.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.toString());
            return apiResponse;
        } catch (Exception e) {
            log.error("Error occurred while processing registration request: {}", e.getMessage());
            apiResponse.setSuccess(false);
            apiResponse.setResponseMessage("An error occurred while processing the registration request.");
            apiResponse.setRequestId(baseRequest.getRequestId());
            apiResponse.setTimestamp(OffsetDateTime.now());
            apiResponse.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.toString());
            return apiResponse;
        }
        return apiResponse;
    }

    private User setUser(RegisterUserRequest registerUserRequest, SecurityConfigDto securityConfigDto) {
        return User.builder()
                .email(normalize(registerUserRequest.getEmail()))
                .mobile(normalize(registerUserRequest.getMobile()))
                .passwordHash(PasswordUtility.hashPassword(registerUserRequest.getPassword()))
                .passwordAlgo(passwordAlgo)
                .isActive(false)
                .accountExpiresAt(OffsetDateTime.now().plusDays(90))
                .lockedUntil(null)
                .lockReason(null)
                .isMfaEnabled(securityConfigDto.isMfaRequired())
                .mfaMethod(registerUserRequest.getMfaMethod())
                .authenticationMethod("JWT")
                .failedLoginCount(0)
                .lastFailedLoginAt(null)
                .lastLoginAt(null)
                .passwordChangedAt(OffsetDateTime.now())
                .passwordExpiresAt(OffsetDateTime.now().plusDays(securityConfigDto.getPasswordMaxAgeDays()))
                .mustChangePassword(false)
                .securityPolicy(Mapper.toEntity(securityConfigDto))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private PasswordHistory setPasswordHistory(User user){
        return PasswordHistory.builder()
                .user(user)
                .passwordHash(user.getPasswordHash())
                .passwordAlgo(user.getPasswordAlgo())
                .changedAt(OffsetDateTime.now())
                .build();
    }

    private void publishRegistrationOtpNotification(User savedUser, BaseRequest baseRequest) {
        registrationOtpService.issueOtp(savedUser, baseRequest, "auth-register", false, null);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalize(String value) {
        return isNotBlank(value) ? value.trim() : null;
    }
}
