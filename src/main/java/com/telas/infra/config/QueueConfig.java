package com.telas.infra.config;

import com.telas.infra.components.QueueProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class QueueConfig {
  private final QueueProperties queueProperties;

  @Bean
  public TopicExchange exchange() {
    return new TopicExchange(queueProperties.getExchangeName());
  }

  @Bean
  public Queue queue() {
    return new Queue(queueProperties.getQueueName(), true);
  }

  @Bean
  public Binding binding(Queue queue, TopicExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(queueProperties.getRoutingKey());
  }
}
