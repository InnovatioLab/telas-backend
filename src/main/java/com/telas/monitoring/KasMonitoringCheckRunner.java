package com.telas.monitoring;

import com.telas.services.SmartPlugCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KasMonitoringCheckRunner {

    private final SmartPlugCheckService smartPlugCheckService;

    public Map<String, Object> runKasaChecks() {
        return smartPlugCheckService.runAllChecks();
    }
}
