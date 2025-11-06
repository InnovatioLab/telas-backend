package com.telas.services.impl;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.telas.dtos.EmailDataDto;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.services.EmailService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.ContactValidationMessages;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;

@Log4j2
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final Configuration freemarkerConfig;

    @Value("${spring.mail.username}")
    private String emailFrom;

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Override
    @Async
    public void send(EmailDataDto data) {
        try {
            Template template = freemarkerConfig.getTemplate(data.getTemplate());
            String conteudoEmail = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfig.getTemplate(data.getTemplate()), data.getParams());

            Email from = new Email(SharedConstants.EMAIL_SENDER, "Telas");
            Email toEmail = new Email(data.getEmail());
            Content content = new Content("text/html", conteudoEmail);
            Mail mail = new Mail(from, data.getSubject(), toEmail, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            if (response.getStatusCode() >= 400 && response.getStatusCode() <= 500) {
                log.error("Error while sending email: {}", response.getBody());
                throw new BusinessRuleException(ContactValidationMessages.ERRO_WHILE_SENDING_EMAIL);
            }

            log.info("Message sent to email {}", data.getEmail());
        } catch (IOException | TemplateException ex) {
            log.error("Error while sending email: {}", ex.getMessage());
            throw new BusinessRuleException(ContactValidationMessages.ERRO_WHILE_SENDING_EMAIL);
        }
    }


}
