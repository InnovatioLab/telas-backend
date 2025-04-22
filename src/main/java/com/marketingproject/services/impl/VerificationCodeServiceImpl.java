package com.marketingproject.services.impl;

import com.marketingproject.dtos.EmailDataDto;
import com.marketingproject.dtos.MessagingDataDto;
import com.marketingproject.entities.Client;
import com.marketingproject.entities.VerificationCode;
import com.marketingproject.enums.CodeType;
import com.marketingproject.enums.ContactPreference;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.repositories.VerificationCodeRepository;
import com.marketingproject.services.EmailService;
import com.marketingproject.services.SmsService;
import com.marketingproject.services.VerificationCodeService;
import com.marketingproject.shared.constants.valitation.ClientValidationMessages;
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

        if (invalidCode || expiredCode) {
            throw new BusinessRuleException(ClientValidationMessages.INVALID_OR_EXPIRED_CODE);
        }

        client.getVerificationCode().setValidated(true);
    }

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
