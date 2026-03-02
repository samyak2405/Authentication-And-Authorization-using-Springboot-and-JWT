package com.javaproject.application.service.impl;

import com.javaproject.application.dto.notification.NotificationEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.notifications.exchange:notifications.events}")
    private String exchange;

    @Value("${app.notifications.routing-key.email:email.send}")
    private String emailRoutingKey;

    public void publishRegistrationOtpEvent(NotificationEventMessage message, String correlationId) {
        try {
            MessagePostProcessor messagePostProcessor = rabbitMessage -> {
                if (correlationId != null && !correlationId.isBlank()) {
                    rabbitMessage.getMessageProperties().setHeader(CORRELATION_HEADER, correlationId);
                }
                return rabbitMessage;
            };
            rabbitTemplate.convertAndSend(exchange, emailRoutingKey, message, messagePostProcessor);
            log.info("Published notification event. exchange={}, routingKey={}, notificationMessage={}, messagePostProcessor:{}",
                    exchange, emailRoutingKey, message.toString(), messagePostProcessor);
        } catch (Exception ex) {
            // Registration flow should not fail because of downstream async notification delivery.
            log.error("Failed to publish notification event for userId={}. Error={}",
                    message.getUserId(), ex.getMessage(), ex);
        }
    }
}
