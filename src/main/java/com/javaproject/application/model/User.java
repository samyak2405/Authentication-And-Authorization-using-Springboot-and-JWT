package com.javaproject.application.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name = "users")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Column(name = "mobile", unique = true, length = 20)
    private String mobile;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_algo", nullable = false, length = 32)
    private String passwordAlgo;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "account_expires_at")
    private OffsetDateTime accountExpiresAt;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "lock_reason", length = 255)
    private String lockReason;

    @Column(name = "is_mfa_enabled", nullable = false)
    private boolean isMfaEnabled;

    @Column(name = "mfa_method", length = 32)
    private String mfaMethod;

    @Column(name = "authentication_method", length = 32)
    private String authenticationMethod;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "last_failed_login_at")
    private OffsetDateTime lastFailedLoginAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private OffsetDateTime passwordChangedAt;

    @Column(name = "password_expires_at")
    private OffsetDateTime passwordExpiresAt;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_policy_id")
    private SecurityPolicy securityPolicy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
