package com.telas.services.impl;

import com.telas.dtos.request.SmartPlugIngestRequestDto;
import com.telas.monitoring.entities.CheckRunEntity;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.PlugReading;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.monitoring.repositories.CheckRunEntityRepository;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import com.telas.services.SmartPlugIngestService;
import com.telas.services.SmartPlugRulesService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmartPlugIngestServiceImpl implements SmartPlugIngestService {

    private static final Logger log = LoggerFactory.getLogger(SmartPlugIngestServiceImpl.class);

    private final SmartPlugEntityRepository smartPlugEntityRepository;
    private final CheckRunEntityRepository checkRunEntityRepository;
    private final SmartPlugRulesService smartPlugRulesService;
    private final BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;

    @Value("${monitoring.heartbeat.stale-seconds:180}")
    private long staleSeconds;

    @Override
    @Transactional
    public void ingest(SmartPlugIngestRequestDto dto) {
        SmartPlugEntity plug = resolvePlug(dto);
        if (plug == null) {
            log.warn("smartPlug.ingest.unknownPlug boxId={} plugId={} mac={}", dto.getBoxId(), dto.getSmartPlugId(), dto.getMacAddress());
            return;
        }

        if (dto.getResolvedIp() != null && !dto.getResolvedIp().isBlank()) {
            plug.setLastSeenIp(dto.getResolvedIp().trim());
            plug.setUpdatedAt(Instant.now());
            smartPlugEntityRepository.save(plug);
        }

        PlugReading reading =
                new PlugReading(
                        Boolean.TRUE.equals(dto.getReachable()),
                        dto.getRelayOn(),
                        dto.getPowerWatts(),
                        dto.getVoltageVolts(),
                        dto.getCurrentAmperes(),
                        dto.getErrorCode());

        persistCheckRun(plug, dto.getAt(), reading);

        boolean heartbeatStale = isHeartbeatStale(dto.getBoxId());
        smartPlugRulesService.evaluate(plug, reading, heartbeatStale);
    }

    private SmartPlugEntity resolvePlug(SmartPlugIngestRequestDto dto) {
        UUID plugId = dto.getSmartPlugId();
        if (plugId != null) {
            Optional<SmartPlugEntity> p = smartPlugEntityRepository.findById(plugId);
            if (p.isPresent()) {
                return p.get();
            }
        }
        String macHex = dto.getMacAddress() != null
                ? dto.getMacAddress().replace(":", "").replace("-", "").trim().toUpperCase()
                : "";
        if (!macHex.isBlank()) {
            return smartPlugEntityRepository.findByMacAddress(macHex).orElse(null);
        }
        return null;
    }

    private void persistCheckRun(SmartPlugEntity plug, Instant at, PlugReading reading) {
        CheckRunEntity run = new CheckRunEntity();
        run.setSmartPlug(plug);
        run.setStartedAt(at != null ? at : Instant.now());
        run.setFinishedAt(at != null ? at : Instant.now());
        run.setSuccess(reading.reachable());
        if (!reading.reachable()) {
            run.setErrorMessage(reading.errorCode());
        }
        Map<String, Object> meta = new HashMap<>();
        meta.put("relayOn", reading.relayOn());
        meta.put("powerWatts", reading.powerWatts());
        meta.put("voltageVolts", reading.voltageVolts());
        meta.put("currentAmperes", reading.currentAmperes());
        meta.put("confidence", "agent");
        run.setMetadataJson(meta);
        checkRunEntityRepository.save(run);
    }

    private boolean isHeartbeatStale(UUID boxId) {
        if (boxId == null) {
            return true;
        }
        Instant cutoff = Instant.now().minusSeconds(staleSeconds);
        return boxHeartbeatEntityRepository
                .findByBox_Id(boxId)
                .map(h -> h.getLastSeenAt().isBefore(cutoff))
                .orElse(true);
    }
}

