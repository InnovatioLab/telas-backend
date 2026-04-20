package com.telas.services.impl;

import com.telas.dtos.response.SmartPlugHistoryPointResponseDto;
import com.telas.dtos.response.SmartPlugOverviewResponseDto;
import com.telas.entities.Address;
import com.telas.entities.Box;
import com.telas.entities.BoxAddress;
import com.telas.entities.Monitor;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.repositories.SmartPlugCheckRunRepository;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import com.telas.services.SmartPlugOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmartPlugOverviewServiceImpl implements SmartPlugOverviewService {

    private final SmartPlugEntityRepository smartPlugEntityRepository;
    private final SmartPlugCheckRunRepository smartPlugCheckRunRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SmartPlugOverviewResponseDto> overview() {
        List<SmartPlugEntity> plugs = smartPlugEntityRepository.findAllEnabledForOverview();
        Map<UUID, SmartPlugCheckRunRepository.SmartPlugLastReadingRow> byPlug =
                indexLastReadings(smartPlugCheckRunRepository.findLastReadingsForAllPlugs());

        return plugs.stream()
                .map(
                        p -> {
                            SmartPlugCheckRunRepository.SmartPlugLastReadingRow last = byPlug.get(p.getId());
                            Monitor m = p.getMonitor();
                            Box box = resolveBox(p);
                            String monitorAddressSummary = resolveMonitorAddressSummary(m);
                            String boxIp = resolveBoxIp(box);
                            return SmartPlugOverviewResponseDto.builder()
                                    .id(p.getId())
                                    .macAddress(p.getMacAddress())
                                    .vendor(p.getVendor())
                                    .model(p.getModel())
                                    .displayName(p.getDisplayName())
                                    .monitorId(m != null ? m.getId() : null)
                                    .monitorAddressSummary(monitorAddressSummary)
                                    .boxId(box != null ? box.getId() : null)
                                    .boxIp(boxIp)
                                    .enabled(p.isEnabled())
                                    .lastSeenIp(p.getLastSeenIp())
                                    .accountEmail(p.getAccountEmail())
                                    .passwordConfigured(
                                            p.getPasswordCipher() != null && !p.getPasswordCipher().isBlank())
                                    .lastReadingAt(last != null ? last.getStartedAt() : null)
                                    .reachable(last != null && Boolean.TRUE.equals(last.getSuccess()))
                                    .relayOn(last != null ? last.getRelayOn() : null)
                                    .powerWatts(last != null ? last.getPowerWatts() : null)
                                    .voltageVolts(last != null ? last.getVoltageVolts() : null)
                                    .currentAmperes(last != null ? last.getCurrentAmperes() : null)
                                    .errorCode(last != null ? last.getErrorMessage() : null)
                                    .build();
                        })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmartPlugHistoryPointResponseDto> history(UUID plugId, Instant from, Instant to, int limit) {
        int safeLimit = limit <= 0 ? 200 : Math.min(limit, 2000);
        return smartPlugCheckRunRepository.findHistory(plugId, from, to, safeLimit).stream()
                .map(
                        r ->
                                SmartPlugHistoryPointResponseDto.builder()
                                        .at(r.getStartedAt())
                                        .reachable(Boolean.TRUE.equals(r.getSuccess()))
                                        .relayOn(r.getRelayOn())
                                        .powerWatts(r.getPowerWatts())
                                        .voltageVolts(r.getVoltageVolts())
                                        .currentAmperes(r.getCurrentAmperes())
                                        .errorCode(r.getErrorMessage())
                                        .build())
                .toList();
    }

    private static Map<UUID, SmartPlugCheckRunRepository.SmartPlugLastReadingRow> indexLastReadings(
            List<SmartPlugCheckRunRepository.SmartPlugLastReadingRow> rows) {
        Map<UUID, SmartPlugCheckRunRepository.SmartPlugLastReadingRow> m = new HashMap<>();
        for (SmartPlugCheckRunRepository.SmartPlugLastReadingRow r : rows) {
            if (r.getSmartPlugId() != null) {
                m.put(r.getSmartPlugId(), r);
            }
        }
        return m;
    }

    private static Box resolveBox(SmartPlugEntity p) {
        if (p.getMonitor() != null) {
            return p.getMonitor().getBox();
        }
        return p.getBox();
    }

    private static String resolveMonitorAddressSummary(Monitor m) {
        if (m == null) {
            return null;
        }
        Address a = m.getAddress();
        return a != null ? a.getCoordinatesParams() : null;
    }

    private static String resolveBoxIp(Box box) {
        if (box == null) {
            return null;
        }
        BoxAddress addr = box.getBoxAddress();
        return addr != null ? addr.getIp() : null;
    }
}

