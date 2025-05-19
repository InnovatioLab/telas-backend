package com.telas.dtos.response;

import com.stripe.model.PaymentIntent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfoResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 5288515525105234502L;

  private String paymentIntentId;

  private String clientSecret;

  private String status;

  public PaymentInfoResponseDto(PaymentIntent paymentIntent) {
    paymentIntentId = paymentIntent.getId();
    clientSecret = paymentIntent.getClientSecret();
    status = paymentIntent.getStatus();
  }
}
