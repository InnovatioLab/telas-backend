package com.telas.dtos.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.telas.shared.constants.valitation.PaymentValidationMessages;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatePaymentStatusRequestDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -3963846843873646628L;

  @NotEmpty(message = PaymentValidationMessages.ID_CANNOT_BE_EMPTY)
  private String id;

  @NotEmpty(message = PaymentValidationMessages.STATUS_CANNOT_BE_EMPTY)
  private String status;
}