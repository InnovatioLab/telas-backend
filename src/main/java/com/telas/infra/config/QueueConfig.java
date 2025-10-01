package com.telas.infra.config;

import com.telas.infra.components.QueueProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
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
        return QueueBuilder.durable(queueProperties.getQueueName())
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .build();
    }


    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(queueProperties.getRoutingKey());
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange("dlx.exchange");
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dlx.queue").build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("#");
    }
}
