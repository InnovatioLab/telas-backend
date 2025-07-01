package com.telas.services.impl;

import com.telas.entities.Client;
import com.telas.entities.Notification;
import com.telas.enums.NotificationReference;
import com.telas.repositories.NotificationRepository;
import com.telas.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
  private final NotificationRepository notificationRepository;
//  private final EmailService emailService;

  @Override
  @Transactional
  public void save(NotificationReference notificationReference, Client client, Map<String, String> params) {
    Notification notification = new Notification(notificationReference, client, params);
    notificationRepository.save(notification);
  }

//  @Override
//  @Transactional
//  public void notify(NotificationReference notificationReference, Client client, Map<String, String> params) {
//    Contact contact = client.getContact();
//    EmailDataDto emailData = notificationReference.getEmailData(params);
//    emailData.setEmail(contact.getEmail());
//    emailService.send(emailData);
//  }
}
