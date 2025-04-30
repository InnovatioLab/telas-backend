package com.marketingproject.services.impl;

import com.marketingproject.dtos.EmailDataDto;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.services.EmailService;
import com.marketingproject.shared.constants.SharedConstants;
import com.marketingproject.shared.constants.valitation.ContactValidationMessages;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;

@Log4j2
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender emailSender;
    private final Configuration freemarkerConfig;

    @Value("${spring.mail.username}")
    private String emailFrom;

    @Transactional
    @Override
    public void send(EmailDataDto data) {
        try {
            Template template = freemarkerConfig.getTemplate(data.getTemplate());

            StringWriter stringWriter = new StringWriter();
            template.process(data.getParams(), stringWriter);
            String conteudoEmail = stringWriter.toString();

            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
            InternetAddress sender = new InternetAddress(emailFrom, SharedConstants.EMAIL_SENDER);
            InternetAddress destination = new InternetAddress(data.getEmail(), SharedConstants.DESTINATARIO);

            helper.setSubject(data.getSubject());
            helper.setFrom(sender);
            helper.setTo(destination);
            helper.setText(conteudoEmail, true);
            emailSender.send(mimeMessage);
            log.info("Message sent to email {}", data.getEmail());
        } catch (MailException | MessagingException | IOException | TemplateException e) {
            log.error("Error while sending email: {}", e.getMessage());
            throw new BusinessRuleException(ContactValidationMessages.ERRO_WHILE_SENDING_EMAIL);
        }
    }
}
