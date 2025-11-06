package com.telas.services;

import com.telas.dtos.EmailDataDto;
import com.telas.entities.Client;
import com.telas.entities.VerificationCode;
import com.telas.enums.CodeType;

public interface VerificationCodeService {
  VerificationCode save(CodeType type, Client client);

  void validate(Client client, String code);

  void send(EmailDataDto emailData);
}
