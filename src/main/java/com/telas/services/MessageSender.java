package com.telas.services;

import com.stripe.model.Event;

public interface MessageSender {
  void sendEvent(Event event);
}
