package com.telas.services.impl;

import com.telas.dtos.EmailDataDto;
import com.telas.entities.Client;
import com.telas.entities.Contact;
import com.telas.entities.Notification;
import com.telas.enums.ContactPreference;
import com.telas.enums.NotificationReference;
import com.telas.repositories.NotificationRepository;
import com.telas.services.EmailService;
import com.telas.services.NotificationService;
import com.telas.services.SmsService;
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
