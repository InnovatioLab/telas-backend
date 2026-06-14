package com.telas.services.impl;

import com.telas.dtos.response.BoxOverviewDto;
import com.telas.dtos.response.MonitorSummaryDto;
import com.telas.entities.Address;
import com.telas.entities.Box;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.monitoring.entities.BoxConnectivityProbeEntity;
import com.telas.monitoring.entities.BoxHeartbeatEntity;
import com.telas.monitoring.repositories.BoxConnectivityProbeEntityRepository;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.services.AdminBoxOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBoxOverviewServiceImpl implements AdminBoxOverviewService {

    private final BoxRepository boxRepository;
    private final BoxHeartbeatEntityRepository heartbeatRepository;
    private final BoxConnectivityProbeEntityRepository probeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BoxOverviewDto> listBoxOverview() {
        List<Box> boxes = boxRepository.findAllForTestingOverview();

        List<BoxHeartbeatEntity> allHeartbeats = heartbeatRepository.findAll();
        Map<UUID, BoxHeartbeatEntity> heartbeatByBoxId = allHeartbeats.stream()
                .collect(Collectors.toMap(h -> h.getBox().getId(), h -> h));

        List<BoxConnectivityProbeEntity> allProbes = probeRepository.findAll();
        Map<UUID, BoxConnectivityProbeEntity> probeByBoxId = allProbes.stream()
                .collect(Collectors.toMap(BoxConnectivityProbeEntity::getBoxId, p -> p));

        return boxes.stream()
                .map(box -> toBoxOverviewDto(box, heartbeatByBoxId, probeByBoxId))
                .collect(Collectors.toList());
    }

    private BoxOverviewDto toBoxOverviewDto(
            Box box,
            Map<UUID, BoxHeartbeatEntity> heartbeatByBoxId,
            Map<UUID, BoxConnectivityProbeEntity> probeByBoxId) {

        BoxHeartbeatEntity heartbeat = heartbeatByBoxId.get(box.getId());
        BoxConnectivityProbeEntity probe = probeByBoxId.get(box.getId());

        List<MonitorSummaryDto> monitorSummaries = box.getMonitors().stream()
                .map(this::toMonitorSummary)
                .collect(Collectors.toList());

        int totalActiveAds = monitorSummaries.stream()
                .mapToInt(MonitorSummaryDto::activeAds)
                .sum();

        return new BoxOverviewDto(
                box.getId(),
                box.getBoxAddress() != null ? box.getBoxAddress().getIp() : null,
                box.getBoxAddress() != null ? box.getBoxAddress().getMac() : null,
                box.getBoxAddress() != null ? box.getBoxAddress().getDns() : null,
                box.isActive(),
                heartbeat != null ? heartbeat.getLastSeenAt() : null,
                heartbeat != null ? heartbeat.getReportedVersion() : null,
                probe != null && probe.isReachable(),
                probe != null ? probe.getProbeDetail() : null,
                monitorSummaries.size(),
                totalActiveAds,
                monitorSummaries
        );
    }

    private MonitorSummaryDto toMonitorSummary(Monitor monitor) {
        Address addr = monitor.getAddress();
        String partnerName = null;
        String street = null;
        String city = null;
        Double latitude = null;
        Double longitude = null;

        if (addr != null) {
            street = addr.getStreet();
            city = addr.getCity();
            latitude = addr.getLatitude();
            longitude = addr.getLongitude();
            if (addr.getClient() != null) {
                partnerName = addr.getClient().getBusinessName();
            }
        }

        int activeAds = (int) monitor.getMonitorAds().stream()
                .filter(ma -> ma.getId().getAd() != null
                        && ma.getId().getAd().getOnAirNotifiedAt() != null)
                .count();

        return new MonitorSummaryDto(
                monitor.getId(),
                monitor.getProductId(),
                partnerName,
                street,
                city,
                latitude,
                longitude,
                activeAds
        );
    }
}
