package com.telas.infra.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class PaymentGatewayConfig {
  @Value("${payment.gateway.key}")
  private String apiKey;

  @PostConstruct
  public void configureApiKey() {
    Stripe.apiKey = apiKey;
  }
}
