package com.telas.notification;

import com.telas.dtos.EmailDataDto;
import com.telas.enums.NotificationReference;

import java.util.Map;

public interface NotificationHandler {

    NotificationReference getReference();

    String getNotificationMessage(Map<String, String> params);

    EmailDataDto getEmailData(Map<String, String> params);
}
