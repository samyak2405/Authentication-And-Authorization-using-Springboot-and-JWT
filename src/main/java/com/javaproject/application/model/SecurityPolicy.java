package com.javaproject.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name = "security_policies")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityPolicy {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 64)
    private String name;

    @Column(name = "password_min_length", nullable = false)
    private int passwordMinLength;

    @Column(name = "password_max_age_days")
    private Integer passwordMaxAgeDays;

    @Column(name = "password_history_count", nullable = false)
    private int passwordHistoryCount;

    @Column(name = "lockout_threshold", nullable = false)
    private int lockoutThreshold;

    @Column(name = "lockout_duration_minutes", nullable = false)
    private int lockoutDurationMinutes;

    @Column(name = "mfa_required", nullable = false)
    private boolean mfaRequired;

    @Column(name = "password_expiry_warning_days", nullable = false)
    private int passwordExpiryWarningDays;

    @Column(name = "password_expiry_days", nullable = false)
    private int passwordExpiryDays;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
