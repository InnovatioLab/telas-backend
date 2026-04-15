package com.telas.monitoring;

import com.telas.services.SmartPlugCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KasMonitoringCheckRunner {

    private final SmartPlugCheckService smartPlugCheckService;

    public void runKasaChecks() {
        smartPlugCheckService.runAllChecks();
    }
}
