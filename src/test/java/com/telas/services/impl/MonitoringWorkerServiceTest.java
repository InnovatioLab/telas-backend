package com.telas.services.impl;

import com.telas.entities.Box;
import com.telas.entities.BoxAddress;
import com.telas.monitoring.KasMonitoringCheckRunner;
import com.telas.monitoring.entities.BoxHeartbeatEntity;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.scheduler.SchedulerJobRunContext;
import com.telas.services.SmartPlugIpDiscoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringWorkerServiceTest {

    @Mock private BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;
    @Mock private BoxRepository boxRepository;
    @Mock private KasMonitoringCheckRunner kasMonitoringCheckRunner;
    @Mock private SmartPlugIpDiscoveryService smartPlugIpDiscoveryService;
    @Mock private SchedulerJobRunContext schedulerJobRunContext;

    private MonitoringWorkerService service;

    @BeforeEach
    void setUp() {
        service = new MonitoringWorkerService(
                boxHeartbeatEntityRepository,
                boxRepository,
                kasMonitoringCheckRunner,
                smartPlugIpDiscoveryService,
                schedulerJobRunContext);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "staleSeconds", 180L);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "neverSeenGraceSeconds", 600L);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "kasaEnabled", false);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "probeDrivesBoxActiveState", false);
    }

    @Test
    void runChecks_neverSeen_recordsSummaryWithoutDeactivation() {
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

        verify(schedulerJobRunContext).put(eq("neverSeenHeartbeatsProcessed"), eq(0));
        verify(schedulerJobRunContext, atLeastOnce()).put(eq("neverSeenHeartbeats"), any());
    }

    @Test
    void runChecks_staleHeartbeat_recordsSummaryWithoutDeactivation() {
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

        verify(schedulerJobRunContext).put(eq("staleHeartbeatsProcessed"), eq(0));
        verify(schedulerJobRunContext, atLeastOnce()).put(eq("staleHeartbeats"), any());
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

        verify(schedulerJobRunContext).put(eq("staleHeartbeatsProcessed"), eq(0));
    }

    @Test
    void runChecks_skipsHeartbeatQueriesWhenProbeDrivesBoxState() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "probeDrivesBoxActiveState", true);

        service.runChecks();

        verify(boxHeartbeatEntityRepository, never()).findStaleHeartbeats(any());
        verify(boxRepository, never()).findActiveBoxesWithoutHeartbeatAfterGrace(any());
    }
}
