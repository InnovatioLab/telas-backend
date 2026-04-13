package com.telas.services.impl;

import com.telas.dtos.EmailDataDto;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.services.ApplicationLogService;
import com.telas.services.EmailService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.ContactValidationMessages;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String EMAIL_LOG_SOURCE = "EMAIL";
    private static final int MAX_PARAM_VALUE_LEN = 800;
    private static final int MAX_ERR_MSG = 1500;

    private final JavaMailSender emailSender;
    private final Configuration freemarkerConfig;
    private final ApplicationLogService applicationLogService;

    @Value("${spring.mail.username}")
    private String emailFrom;

    @Override
    @Async
    public void send(EmailDataDto data) {
        try {
            Template template = freemarkerConfig.getTemplate(data.getTemplate());

            StringWriter stringWriter = new StringWriter();
            template.process(data.getParams(), stringWriter);
            String conteudoEmail = stringWriter.toString();

            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
            InternetAddress sender = new InternetAddress(emailFrom, SharedConstants.EMAIL_SENDER);
            InternetAddress destination = new InternetAddress(data.getEmail(), SharedConstants.RECIPIENT);

            helper.setSubject(data.getSubject());
            helper.setFrom(sender);
            helper.setTo(destination);
            helper.setText(conteudoEmail, true);
            emailSender.send(mimeMessage);
            log.info("Message sent to email {}", data.getEmail());
            recordEmailInApplicationLogs(data, true, null);
        } catch (MailException | MessagingException | IOException | TemplateException e) {
            log.error("Error while sending email: {}", e.getMessage());
            recordEmailInApplicationLogs(data, false, e);
            throw new BusinessRuleException(ContactValidationMessages.ERRO_WHILE_SENDING_EMAIL);
        }
    }

    private void recordEmailInApplicationLogs(EmailDataDto data, boolean success, Exception error) {
        try {
            Map<String, Object> metadata = buildEmailLogMetadata(data);
            if (!success && error != null) {
                metadata.put(
                        "errorType",
                        error.getClass().getSimpleName());
                metadata.put(
                        "errorMessage",
                        truncate(error.getMessage() != null ? error.getMessage() : error.toString(), MAX_ERR_MSG));
            }
            metadata.put("status", success ? "SENT" : "FAILED");

            String recipient = data.getEmail() != null ? data.getEmail() : "";
            String subject = data.getSubject() != null ? data.getSubject() : "";
            String template = data.getTemplate() != null ? data.getTemplate() : "";
            String message =
                    success
                            ? String.format(
                                    "Email enviado: destinatário=%s | assunto=%s | template=%s",
                                    recipient,
                                    subject,
                                    template)
                            : String.format(
                                    "Falha ao enviar email: destinatário=%s | assunto=%s | template=%s",
                                    recipient,
                                    subject,
                                    template);

            String level = success ? "INFO" : "ERROR";
            applicationLogService.persistSystemLog(level, message, EMAIL_LOG_SOURCE, metadata);
        } catch (RuntimeException ex) {
            log.warn("Não foi possível registar o email em application_logs: {}", ex.getMessage());
        }
    }

    private static Map<String, Object> buildEmailLogMetadata(EmailDataDto data) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("recipientEmail", data.getEmail());
        meta.put("subject", data.getSubject());
        meta.put("template", data.getTemplate());
        if (data.getParams() != null && !data.getParams().isEmpty()) {
            Map<String, String> copy = new HashMap<>();
            data.getParams()
                    .forEach(
                            (k, v) -> {
                                if (k != null) {
                                    copy.put(k, v == null ? "" : truncate(v, MAX_PARAM_VALUE_LEN));
                                }
                            });
            meta.put("templateParams", copy);
            String name = data.getParams().get("name");
            if (StringUtils.hasText(name)) {
                meta.put("recipientName", name);
            }
            String locations = data.getParams().get("locations");
            if (StringUtils.hasText(locations)) {
                meta.put("locationsSummary", truncate(locations, MAX_PARAM_VALUE_LEN));
            }
        }
        String clientIdStr = data.getParams() != null ? data.getParams().get("clientId") : null;
        if (StringUtils.hasText(clientIdStr)) {
            meta.put("clientId", clientIdStr.trim());
        }
        return meta;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
