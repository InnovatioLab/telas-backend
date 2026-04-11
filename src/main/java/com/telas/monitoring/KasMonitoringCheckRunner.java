package com.telas.monitoring;

import com.telas.monitoring.entities.CheckDefinitionEntity;
import com.telas.monitoring.repositories.CheckDefinitionEntityRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KasMonitoringCheckRunner {

    private static final Logger log = LoggerFactory.getLogger(KasMonitoringCheckRunner.class);

    private final CheckDefinitionEntityRepository checkDefinitionEntityRepository;

    public void runKasaChecks() {
        checkDefinitionEntityRepository.findByEnabledTrue().stream()
                .filter(d -> "KASA_PLUG".equals(d.getCheckType()))
                .forEach(this::runSingle);
    }

    private void runSingle(CheckDefinitionEntity definition) {
        log.debug("Kasa plug check placeholder for definition id={}", definition.getId());
    }
}
