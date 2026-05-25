package com.telas.notification;

import com.telas.dtos.EmailDataDto;
import com.telas.shared.constants.SharedConstants;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationTemplateRegistry {

    public record EmailTemplate(String subject, String templatePath) {}

    public EmailTemplate clientAdReceived() {
        return new EmailTemplate(
                SharedConstants.EMAIL_SUBJECT_CLIENT_AD_RECEIVED,
                SharedConstants.TEMPLATE_EMAIL_CLIENT_AD_RECEIVED
        );
    }

    public EmailTemplate subscriptionExpiringReminder() {
        return new EmailTemplate(
                SharedConstants.EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_REMINDER,
                SharedConstants.TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_REMINDER
        );
    }

    public EmailTemplate subscriptionExpiringCountdown(String subject) {
        return new EmailTemplate(subject, SharedConstants.TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_COUNTDOWN);
    }

    public EmailDataDto buildEmail(EmailTemplate template, Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(template.subject());
        emailData.setTemplate(template.templatePath());
        emailData.setParams(new HashMap<>(params));
        return emailData;
    }
}
