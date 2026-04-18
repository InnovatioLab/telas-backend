package com.telas.services.impl;

import com.telas.dtos.response.MonitoringTestingRowDto;
import com.telas.entities.Box;
import com.telas.entities.BoxAddress;
import com.telas.enums.BoxScriptVersionStatus;
import com.telas.monitoring.entities.BoxHeartbeatEntity;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.services.BoxTailscalePingOutcome;
import com.telas.services.BoxTailscalePingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringTestingServiceImplTest {

    @Mock private BoxRepository boxRepository;
    @Mock private BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;
    @Mock private SmartPlugEntityRepository smartPlugEntityRepository;
    @Mock private BoxTailscalePingService boxTailscalePingService;

    private MonitoringTestingServiceImpl service;

    private final UUID boxId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new MonitoringTestingServiceImpl(
                        boxRepository,
                        boxHeartbeatEntityRepository,
                        smartPlugEntityRepository,
                        boxTailscalePingService);
        lenient()
                .when(boxTailscalePingService.pingBoxAddressIp(any()))
                .thenReturn(BoxTailscalePingOutcome.notAttempted("ping_disabled"));
        ReflectionTestUtils.setField(service, "staleSeconds", 180L);
        ReflectionTestUtils.setField(service, "configuredTargetBoxScriptVersion", "1.0.0");
    }

    @Test
    void getOverview_includes_reported_version_and_behind_status() {
        Box box = new Box();
        box.setId(boxId);
        box.setActive(true);
        box.setMonitors(Collections.emptyList());
        BoxAddress addr = new BoxAddress();
        addr.setIp("10.0.0.1");
        box.setBoxAddress(addr);

        BoxHeartbeatEntity hb = new BoxHeartbeatEntity();
        hb.setLastSeenAt(Instant.now());
        hb.setReportedVersion("0.9.0");

        when(boxRepository.findAllForTestingOverview()).thenReturn(List.of(box));
        when(boxHeartbeatEntityRepository.findByBox_Id(boxId)).thenReturn(Optional.of(hb));
        when(smartPlugEntityRepository.findAllWithMonitor()).thenReturn(Collections.emptyList());

        List<MonitoringTestingRowDto> rows = service.getOverview();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getReportedBoxScriptVersion()).isEqualTo("0.9.0");
        assertThat(rows.get(0).getTargetBoxScriptVersion()).isEqualTo("1.0.0");
        assertThat(rows.get(0).getBoxScriptVersionStatus()).isEqualTo(BoxScriptVersionStatus.BEHIND);
    }
}
