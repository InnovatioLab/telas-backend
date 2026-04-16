package com.telas.scheduler;

import com.telas.dtos.EmailDataDto;
import com.telas.repositories.ClientRepository;
import com.telas.services.EmailService;
import com.telas.shared.constants.SharedConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SchedulerPreRunNotificationService {

    private final ClientRepository clientRepository;
    private final EmailService emailService;

    @Value("${scheduler.prerun.email.enabled:true}")
    private boolean emailEnabled;

    public void notifyCronStarting(String jobId) {
        if (!emailEnabled) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("jobId", jobId);
        params.put("startedAt", Instant.now().toString());
        clientRepository
                .findAllAdmins()
                .forEach(
                        admin -> {
                            String to = admin.getContact() != null ? admin.getContact().getEmail() : null;
                            if (to == null || to.isBlank()) {
                                return;
                            }
                            EmailDataDto data = new EmailDataDto();
                            data.setEmail(to);
                            data.setSubject(SharedConstants.EMAIL_SUBJECT_SCHEDULER_PRERUN);
                            data.setTemplate(SharedConstants.TEMPLATE_EMAIL_SCHEDULER_PRERUN);
                            data.setParams(new HashMap<>(params));
                            emailService.send(data);
                        });
    }
}
