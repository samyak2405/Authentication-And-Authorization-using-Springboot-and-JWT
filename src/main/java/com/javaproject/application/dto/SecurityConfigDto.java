package com.javaproject.application.dto;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SecurityConfigDto implements Serializable {

    private UUID id;
    private String name;
    private int passwordMinLength;
    private Integer passwordMaxAgeDays;
    private int passwordHistoryCount;
    private int lockoutThreshold;
    private int lockoutDurationMinutes;
    private boolean isMfaRequired;
    private int passwordExpiryWarningDays;
    private int accountExpiryDays;
}
