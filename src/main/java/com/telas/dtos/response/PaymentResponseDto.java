package com.telas.dtos.response;

import com.telas.entities.Payment;
import com.telas.enums.PaymentStatus;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
public final class PaymentResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -2929124221854520175L;

  private final UUID id;

  private final BigDecimal amount;

  private final String paymentMethod;

  private final String currency;

  private final PaymentStatus status;

  public PaymentResponseDto(Payment entity) {
    id = entity.getId();
    amount = entity.getAmount();
    paymentMethod = entity.getPaymentMethod();
    currency = entity.getCurrency();
    status = entity.getStatus();
  }
}