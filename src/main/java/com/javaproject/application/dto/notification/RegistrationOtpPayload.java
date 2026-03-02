package com.javaproject.application.dto.notification;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RegistrationOtpPayload {
    private String email;
    private String otp;
    private int expiryMinutes;
}
