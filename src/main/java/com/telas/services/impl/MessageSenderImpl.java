package com.telas.services.impl;

import com.stripe.model.Event;
import com.telas.infra.components.QueueProperties;
import com.telas.services.MessageSender;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageSenderImpl implements MessageSender {
  private final Logger log = LoggerFactory.getLogger(MessageSenderImpl.class);
  private final RabbitTemplate rabbitTemplate;
  private final QueueProperties queueProperties;

  @Override
  public void sendEvent(Event event) {
    if (event == null || event.getId() == null) {
      log.error("Attempted to send a null or invalid event");
      return;
    }

    try {
      String eventJson = event.toJson();
      log.info("[SENDER]: Sending event with ID: {} and type: {}", event.getId(), event.getType());
      rabbitTemplate.convertAndSend(queueProperties.getExchangeName(), queueProperties.getRoutingKey(), eventJson);
      log.info("[SENDER]: Event with ID: {} sent successfully", event.getId());
    } catch (Exception e) {
      log.error("Failed to serialize event: {}", e.getMessage());
    }
  }
}
