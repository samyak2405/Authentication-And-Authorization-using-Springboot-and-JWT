package com.javaproject.application.mapper;

import com.javaproject.application.dto.SecurityConfigDto;
import com.javaproject.application.model.SecurityPolicy;

public class Mapper {
    public static SecurityConfigDto toDto(SecurityPolicy dbConfig) {
        return SecurityConfigDto.builder()
                .id(dbConfig.getId())
                .name(dbConfig.getName())
                .passwordMinLength(dbConfig.getPasswordMinLength())
                .passwordMaxAgeDays(dbConfig.getPasswordMaxAgeDays())
                .passwordHistoryCount(dbConfig.getPasswordHistoryCount())
                .lockoutThreshold(dbConfig.getLockoutThreshold())
                .lockoutDurationMinutes(dbConfig.getLockoutDurationMinutes())
                .isMfaRequired(dbConfig.isMfaRequired())
                .passwordMaxAgeDays(dbConfig.getPasswordExpiryDays())
                .passwordExpiryWarningDays(dbConfig.getPasswordExpiryWarningDays())
                .build();
    }

    public static SecurityPolicy toEntity(SecurityConfigDto securityConfigDto) {
        return SecurityPolicy.builder()
                .id(securityConfigDto.getId())
                .name(securityConfigDto.getName())
                .passwordMinLength(securityConfigDto.getPasswordMinLength())
                .passwordMaxAgeDays(securityConfigDto.getPasswordMaxAgeDays())
                .passwordHistoryCount(securityConfigDto.getPasswordHistoryCount())
                .lockoutThreshold(securityConfigDto.getLockoutThreshold())
                .lockoutDurationMinutes(securityConfigDto.getLockoutDurationMinutes())
                .mfaRequired(securityConfigDto.isMfaRequired())
                .passwordExpiryWarningDays(securityConfigDto.getPasswordExpiryWarningDays())
                .passwordExpiryDays(securityConfigDto.getPasswordMaxAgeDays())
                .build();
    }
}
