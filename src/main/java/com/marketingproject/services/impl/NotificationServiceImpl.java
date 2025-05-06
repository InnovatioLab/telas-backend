package com.marketingproject.services.impl;

import com.marketingproject.dtos.EmailDataDto;
import com.marketingproject.entities.Client;
import com.marketingproject.entities.Contact;
import com.marketingproject.entities.Notification;
import com.marketingproject.enums.ContactPreference;
import com.marketingproject.enums.NotificationReference;
import com.marketingproject.repositories.NotificationRepository;
import com.marketingproject.services.EmailService;
import com.marketingproject.services.NotificationService;
import com.marketingproject.services.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    @Override
    @Transactional
    public void save(NotificationReference notificationReference, Client client, Map<String, String> params) {
        Notification notification = new Notification(notificationReference, client, params);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void notify(NotificationReference notificationReference, Client client, Map<String, String> params) {
        Contact contact = client.getContact();

        if (ContactPreference.EMAIL.equals(contact.getContactPreference())) {
            EmailDataDto emailData = notificationReference.getEmailData(params);
            emailData.setEmail(contact.getEmail());
            emailService.send(emailData);
        } else {
            String message = notificationReference.getPhoneMessage(params);
//            smsService.notify();
        }

    }
}
