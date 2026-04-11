package com.telas.services.impl;

import com.telas.dtos.request.BoxLogRequestDto;
import com.telas.monitoring.entities.ApplicationLogEntity;
import com.telas.monitoring.repositories.ApplicationLogEntityRepository;
import com.telas.services.ApplicationLogService;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationLogServiceImpl implements ApplicationLogService {

    private static final int MAX_STACK = 8000;

    private final ApplicationLogEntityRepository applicationLogEntityRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistFromHandler(String title, Throwable ex, int httpStatus) {
        ApplicationLogEntity entity = new ApplicationLogEntity();
        entity.setLevel(httpStatus >= 500 ? "ERROR" : "WARN");
        entity.setMessage(truncate(title + ": " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName()), 4000));
        entity.setSource("API");
        entity.setStackTrace(truncate(stackTraceToString(ex), MAX_STACK));
        applicationLogEntityRepository.save(entity);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistBoxLog(BoxLogRequestDto request) {
        String level = ValidateDataUtils.isNullOrEmptyString(request.getLevel()) ? "ERROR" : request.getLevel().toUpperCase();
        if (!level.matches("TRACE|DEBUG|INFO|WARN|ERROR")) {
            level = "ERROR";
        }
        ApplicationLogEntity entity = new ApplicationLogEntity();
        entity.setLevel(level);
        entity.setMessage(truncate(request.getMessage(), 4000));
        entity.setSource("BOX");
        Map<String, Object> meta = new HashMap<>();
        if (request.getMetadata() != null) {
            meta.putAll(request.getMetadata());
        }
        if (StringUtils.hasText(request.getBoxAddress())) {
            meta.put("boxAddress", request.getBoxAddress().trim());
        }
        entity.setMetadataJson(meta.isEmpty() ? null : meta);
        applicationLogEntityRepository.save(entity);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String stackTraceToString(Throwable ex) {
        java.io.StringWriter sw = new java.io.StringWriter();
        ex.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
