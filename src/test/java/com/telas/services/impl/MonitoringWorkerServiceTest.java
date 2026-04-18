package com.telas.services.impl;

import com.telas.dtos.request.StatusBoxMonitorsRequestDto;
import com.telas.entities.Box;
import com.telas.entities.BoxAddress;
import com.telas.enums.DefaultStatus;
import com.telas.monitoring.KasMonitoringCheckRunner;
import com.telas.monitoring.entities.BoxHeartbeatEntity;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.scheduler.SchedulerJobRunContext;
import com.telas.services.HealthUpdateService;
import com.telas.shared.constants.MonitoringIncidentTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringWorkerServiceTest {

    @Mock private BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;
    @Mock private BoxRepository boxRepository;
    @Mock private HealthUpdateService healthUpdateService;
    @Mock private KasMonitoringCheckRunner kasMonitoringCheckRunner;
    @Mock private SchedulerJobRunContext schedulerJobRunContext;

    private MonitoringWorkerService service;

    @BeforeEach
    void setUp() {
        service = new MonitoringWorkerService(
                boxHeartbeatEntityRepository,
                boxRepository,
                healthUpdateService,
                kasMonitoringCheckRunner,
                schedulerJobRunContext);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "staleSeconds", 180L);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "neverSeenGraceSeconds", 600L);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "kasaEnabled", false);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "probeDrivesBoxActiveState", false);
    }

    @Test
    void runChecks_appliesNeverSeenForActiveBoxWithoutHeartbeatRow() {
        Box box = new Box();
        box.setId(UUID.randomUUID());
        box.setActive(true);
        BoxAddress addr = new BoxAddress();
        addr.setIp("10.0.0.1");
        addr.setMac("00:00:00:00:00:01");
        box.setBoxAddress(addr);
        when(boxRepository.findActiveBoxesWithoutHeartbeatAfterGrace(any())).thenReturn(List.of(box));
        when(boxHeartbeatEntityRepository.findStaleHeartbeats(any())).thenReturn(List.of());

        service.runChecks();

        ArgumentCaptor<StatusBoxMonitorsRequestDto> captor = ArgumentCaptor.forClass(StatusBoxMonitorsRequestDto.class);
        verify(healthUpdateService).applyHealthUpdate(captor.capture());
        assertThat(captor.getValue().getIncidentType()).isEqualTo(MonitoringIncidentTypes.HEARTBEAT_NEVER_SEEN);
        assertThat(captor.getValue().getStatus()).isEqualTo(DefaultStatus.INACTIVE);
    }

    @Test
    void runChecks_staleHeartbeatUsesMonitoringIncidentTypeConstant() {
        BoxHeartbeatEntity hb = new BoxHeartbeatEntity();
        Box box = new Box();
        box.setId(UUID.randomUUID());
        box.setActive(true);
        BoxAddress addr = new BoxAddress();
        addr.setIp("10.0.0.2");
        addr.setMac("00:00:00:00:00:02");
        box.setBoxAddress(addr);
        hb.setBox(box);
        hb.setLastSeenAt(Instant.parse("2020-01-01T00:00:00Z"));

        when(boxHeartbeatEntityRepository.findStaleHeartbeats(any())).thenReturn(List.of(hb));
        when(boxRepository.findActiveBoxesWithoutHeartbeatAfterGrace(any())).thenReturn(List.of());

        service.runChecks();

        ArgumentCaptor<StatusBoxMonitorsRequestDto> captor = ArgumentCaptor.forClass(StatusBoxMonitorsRequestDto.class);
        verify(healthUpdateService).applyHealthUpdate(captor.capture());
        assertThat(captor.getValue().getIncidentType()).isEqualTo(MonitoringIncidentTypes.HEARTBEAT_STALE);
    }

    @Test
    void runChecks_skipsInactiveBoxForStale() {
        BoxHeartbeatEntity hb = new BoxHeartbeatEntity();
        Box box = new Box();
        box.setActive(false);
        hb.setBox(box);

        when(boxHeartbeatEntityRepository.findStaleHeartbeats(any())).thenReturn(List.of(hb));
        when(boxRepository.findActiveBoxesWithoutHeartbeatAfterGrace(any())).thenReturn(List.of());

        service.runChecks();

        verify(healthUpdateService, never()).applyHealthUpdate(any());
    }

    @Test
    void runChecks_skipsHeartbeatDeactivationWhenProbeDrivesBoxState() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "probeDrivesBoxActiveState", true);

        service.runChecks();

        verify(boxHeartbeatEntityRepository, never()).findStaleHeartbeats(any());
        verify(boxRepository, never()).findActiveBoxesWithoutHeartbeatAfterGrace(any());
        verify(healthUpdateService, never()).applyHealthUpdate(any());
    }
}
