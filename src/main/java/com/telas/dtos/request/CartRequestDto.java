package com.telas.dtos.request;


import com.telas.enums.Recurrence;
import com.telas.shared.constants.valitation.CartValidationMessages;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartRequestDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -3963846843873646628L;

  @NotEmpty(message = CartValidationMessages.ITEMS_REQUIRED)
  private @Valid List<CartItemRequestDto> items = new ArrayList<>();

  private Recurrence recurrence = Recurrence.THIRTY_DAYS;
}