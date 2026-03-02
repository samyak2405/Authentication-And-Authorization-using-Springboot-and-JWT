package com.javaproject.application.dto.notification;

import com.javaproject.application.enums.NotificationChannel;
import com.javaproject.application.enums.NotificationEventType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class NotificationEventMessage {
    private String notificationId;
    private String userId;
    private NotificationChannel channel;
    private NotificationEventType eventType;
    private String idempotencyKey;
    private int attempt;
    private String createdAt;
    private RegistrationOtpPayload payload;
}
