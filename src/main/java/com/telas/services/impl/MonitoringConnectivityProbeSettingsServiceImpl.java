package com.telas.services.impl;

import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.monitoring.entities.ConnectivityProbeSettingsEntity;
import com.telas.monitoring.repositories.ConnectivityProbeSettingsRepository;
import com.telas.services.MonitoringConnectivityProbeSettingsService;
import com.telas.scheduler.BoxConnectivityProbeScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MonitoringConnectivityProbeSettingsServiceImpl implements MonitoringConnectivityProbeSettingsService {

    private static final long MIN_INTERVAL_MS = 5000L;
    private static final long MAX_INTERVAL_MS = 86400000L;
    private static final short ROW_ID = 1;

    private final ConnectivityProbeSettingsRepository connectivityProbeSettingsRepository;
    @Lazy
    @Autowired
    private BoxConnectivityProbeScheduler boxConnectivityProbeScheduler;

    @Value("${monitoring.box-connectivity-probe.interval-ms:300000}")
    private long defaultIntervalMs;

    @Override
    @Transactional(readOnly = true)
    public long getIntervalMs() {
        return connectivityProbeSettingsRepository
                .findById(ROW_ID)
                .map(ConnectivityProbeSettingsEntity::getBoxConnectivityProbeIntervalMs)
                .orElse(defaultIntervalMs);
    }

    @Override
    @Transactional
    public long setIntervalMs(long intervalMs) {
        validateInterval(intervalMs);
        ConnectivityProbeSettingsEntity row =
                connectivityProbeSettingsRepository
                        .findById(ROW_ID)
                        .orElseGet(
                                () -> {
                                    ConnectivityProbeSettingsEntity e = new ConnectivityProbeSettingsEntity();
                                    e.setId(ROW_ID);
                                    return e;
                                });
        row.setBoxConnectivityProbeIntervalMs(intervalMs);
        connectivityProbeSettingsRepository.save(row);
        boxConnectivityProbeScheduler.reschedule();
        return intervalMs;
    }

    private static void validateInterval(long intervalMs) {
        if (intervalMs < MIN_INTERVAL_MS || intervalMs > MAX_INTERVAL_MS) {
            throw new BusinessRuleException(
                    "monitoring.connectivity-probe.interval-ms must be between "
                            + MIN_INTERVAL_MS
                            + " and "
                            + MAX_INTERVAL_MS);
        }
    }
}
