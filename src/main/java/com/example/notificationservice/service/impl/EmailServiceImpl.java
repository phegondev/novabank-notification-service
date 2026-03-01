package com.example.notificationservice.service.impl;

import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.enums.NotificationStatus;
import com.example.notificationservice.enums.NotificationType;
import com.example.notificationservice.enums.transaction.TransactionDirection;
import com.example.notificationservice.kafka.dto.BalanceUpdateEvent;
import com.example.notificationservice.kafka.dto.UserRegistrationEvent;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationRepository notificationRepository;


    @Value("${spring.mail.username}")
    private String fromEmail;


    @Override
    public void sendWelcomeEmail(UserRegistrationEvent event) {

        try {

            Context context = new Context();
            context.setVariable("firstName", event.getFirstName());
            context.setVariable("lastName", event.getLastName());
            context.setVariable("email", event.getEmail());
            context.setVariable("accountNumber", event.getAccountNumber());
            context.setVariable("bankName", event.getBankName());

            String htmlEmailTemplate = templateEngine.process("welcome-email", context);

            Notification notificationToSave = Notification.builder()
                    .recipientEmail(event.getEmail())
                    .type(NotificationType.EMAIL)
                    .subject("Welcome to " + event.getBankName() + "Your Account is Ready!")
                    .message(htmlEmailTemplate)
                    .status(NotificationStatus.SENT)
                    .build();

            //send email out
            sendEmailOut(event.getEmail(), notificationToSave.getSubject(), htmlEmailTemplate);

            //save to the notification database table;
            notificationRepository.save(notificationToSave);
            log.info("Welcome email sent successfully");

        } catch (Exception e) {

            log.info("Error sending email out: {}", e.getMessage());

            Notification notificationToSave = Notification.builder()
                    .recipientEmail(event.getEmail())
                    .type(NotificationType.EMAIL)
                    .subject("Welcome to " + event.getBankName() + "Your Account is Ready!")
                    .message("Failed ti send email out")
                    .status(NotificationStatus.FAILED)
                    .build();

            notificationRepository.save(notificationToSave);

            throw new RuntimeException(e.getMessage());

        }

    }

    @Override
    public void sendTransactionAlertEmail(BalanceUpdateEvent event) {

        try{

            Context context = new Context();
            context.setVariable("name", event.getFirstName());
            context.setVariable("bankName", "NOVA BANK");
            context.setVariable("amount", event.getAmount());
            context.setVariable("currency", "USD");
            context.setVariable("reference", event.getReference());
            context.setVariable("accountNumber", event.getAccountNumber());
            context.setVariable("description", event.getDescription() != null ? event.getDescription() : "Bank Transaction");
            context.setVariable("date", LocalDateTime.now().toString());
            context.setVariable("balance", event.getCurrentBalance());

            String templateName;
            String subject;

            if (TransactionDirection.CREDIT.equals(event.getTransactionDirection())){
                templateName = "credit-alert";
                subject = "Credit Alert: [" + event.getReference() + "]";
            }else {
                templateName = "debit-alert";
                subject = "Debit Alert: [" + event.getReference() + "]";

            }

            String htmlEmailTemplate = templateEngine.process(templateName, context);

            Notification notificationToSave = Notification.builder()
                    .recipientEmail(event.getEmail())
                    .type(NotificationType.EMAIL)
                    .subject(subject)
                    .message(htmlEmailTemplate)
                    .status(NotificationStatus.SENT)
                    .build();

            //send the email out
            sendEmailOut(event.getEmail(), subject, htmlEmailTemplate);

            //save to database
            notificationRepository.save(notificationToSave);
            log.info("Email was sent out successfully to {}", event.getEmail());


        }catch (Exception ex){

            log.info("Error sending email out to {}", event.getEmail());

            Notification errorNotificationToSave = Notification.builder()
                    .recipientEmail(event.getEmail())
                    .type(NotificationType.EMAIL)
                    .subject("Transaction Alert Error")
                    .message("Fail to send transaction email for this ref: {} " + event.getReference())
                    .status(NotificationStatus.FAILED)
                    .build();

            notificationRepository.save(errorNotificationToSave);

            throw new RuntimeException(ex.getMessage());
        }

    }


    private void sendEmailOut(String recipientEmail, String subject, String emailTemplate) throws MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                true,
                "UTF-8"

        );
        helper.setFrom(fromEmail);
        helper.setTo(recipientEmail);
        helper.setSubject(subject);
        helper.setText(emailTemplate, true);

        mailSender.send(mimeMessage);
    }
}


















