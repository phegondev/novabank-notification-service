package com.example.notificationservice.kafka.service;


import com.example.notificationservice.kafka.dto.BalanceUpdateEvent;
import com.example.notificationservice.kafka.dto.UserRegistrationEvent;
import com.example.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumerListener {

    private final EmailService emailService;

    @KafkaListener(topics = "user-registered-events", groupId = "notification-group")
    public void consumerUserRegisteredEvent(UserRegistrationEvent event) {
        log.info("Received user registered event");
        try {
            emailService.sendWelcomeEmail(event);

        } catch (Exception e) {

            log.error("Error sending email our: {}", e.getMessage());
        }
    }


    @KafkaListener(topics = "balance-update-notification-events", groupId = "notification-group")
    public void consumerBalanceUpdateEvent(BalanceUpdateEvent event) {
        log.info("Received balance update  event");
        try {
            emailService.sendTransactionAlertEmail(event);

        } catch (Exception e) {

            log.error("Error sending email our: {}", e.getMessage());
        }
    }


}
