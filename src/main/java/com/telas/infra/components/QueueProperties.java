package com.telas.infra.components;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class QueueProperties {
  @Value("${queue.name}")
  private String queueName;

  @Value("${exchange.name}")
  private String exchangeName;

  @Value("${routing.key}")
  private String routingKey;
}