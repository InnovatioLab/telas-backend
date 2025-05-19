package com.telas.services;

import com.telas.dtos.request.UpdatePaymentStatusRequestDto;
import com.telas.dtos.response.PaymentInfoResponseDto;
import com.telas.entities.Subscription;

public interface PaymentService {
  PaymentInfoResponseDto process(Subscription subscription);

  void updatePaymentStatus(UpdatePaymentStatusRequestDto request);
}
