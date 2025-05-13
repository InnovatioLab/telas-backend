package com.telas.services.impl;

import com.telas.dtos.EmailDataDto;
import com.telas.dtos.MessagingDataDto;
import com.telas.entities.Client;
import com.telas.entities.VerificationCode;
import com.telas.enums.CodeType;
import com.telas.enums.ContactPreference;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.repositories.VerificationCodeRepository;
import com.telas.services.EmailService;
import com.telas.services.SmsService;
import com.telas.services.VerificationCodeService;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {
    private final VerificationCodeRepository repository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final Random random = new Random();

    @Override
    @Transactional
    public VerificationCode save(CodeType type, Client client) {
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));
        VerificationCode verificationCode = new VerificationCode(generateCode(), expiresAt, type);
        return repository.save(verificationCode);
    }

    @Override
    @Transactional
    public void validate(Client client, String code) {
        Instant actualDate = Instant.now();
        Instant expiryDate = client.getVerificationCode().getExpiresAt();
        boolean invalidCode = !client.getVerificationCode().getCode().equals(code);
        boolean expiredCode = !actualDate.isBefore(expiryDate);

//        if (invalidCode || expiredCode) {
//            throw new BusinessRuleException(ClientValidationMessages.INVALID_OR_EXPIRED_CODE);
//        }

        client.getVerificationCode().setValidated(true);
    }

    @Transactional
    @Override
    public void send(MessagingDataDto messagingDataDto, String template, String subject) {
        if (ContactPreference.EMAIL.equals(messagingDataDto.getContactPreference())) {
            EmailDataDto emailData = new EmailDataDto(messagingDataDto, template, subject);
            emailService.send(emailData);
        } else {
            smsService.send(messagingDataDto);
        }
    }

    String generateCode() {
        return String.format("%06d", random.nextInt(100000));
    }
}
