package com.telas.services.impl;

import com.telas.entities.Box;
import com.telas.entities.Monitor;
import com.telas.monitoring.entities.IncidentEntity;
import com.telas.monitoring.repositories.IncidentEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.shared.constants.MonitoringIncidentTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeartbeatRecoveryServiceImplTest {

    @Mock private IncidentEntityRepository incidentEntityRepository;
    @Mock private BoxRepository boxRepository;
    @Mock private MonitorRepository monitorRepository;

    @InjectMocks private HeartbeatRecoveryServiceImpl service;

    @Test
    void recover_closesOpenHeartbeatIncidentsAndReactivates() {
        UUID boxId = UUID.randomUUID();
        Box input = new Box();
        input.setId(boxId);
        input.setActive(false);

        IncidentEntity inc = new IncidentEntity();
        inc.setIncidentType(MonitoringIncidentTypes.HEARTBEAT_STALE);
        when(incidentEntityRepository.findAllByBox_IdAndIncidentTypeInAndClosedAtIsNull(
                        eq(boxId),
                        eq(
                                List.of(
                                        MonitoringIncidentTypes.HEARTBEAT_STALE,
                                        MonitoringIncidentTypes.HEARTBEAT_NEVER_SEEN,
                                        MonitoringIncidentTypes.CONNECTIVITY_PROBE_FAILED))))
                .thenReturn(List.of(inc));

        Box managed = new Box();
        managed.setId(boxId);
        managed.setActive(false);
        Monitor m = new Monitor();
        m.setActive(false);
        List<Monitor> monitors = new ArrayList<>();
        monitors.add(m);
        managed.setMonitors(monitors);

        when(boxRepository.findById(boxId)).thenReturn(Optional.of(managed));

        service.recoverAfterSuccessfulHeartbeat(input);

        ArgumentCaptor<IncidentEntity> incCaptor = ArgumentCaptor.forClass(IncidentEntity.class);
        verify(incidentEntityRepository).save(incCaptor.capture());
        assertThat(incCaptor.getValue().getClosedAt()).isNotNull();

        verify(monitorRepository).saveAll(any());
        verify(boxRepository).save(managed);
        assertThat(managed.isActive()).isTrue();
        assertThat(m.isActive()).isTrue();
    }

    @Test
    void recover_noOpWhenNoOpenIncidents() {
        UUID boxId = UUID.randomUUID();
        Box input = new Box();
        input.setId(boxId);
        when(incidentEntityRepository.findAllByBox_IdAndIncidentTypeInAndClosedAtIsNull(
                        eq(boxId), any()))
                .thenReturn(List.of());

        service.recoverAfterSuccessfulHeartbeat(input);

        verify(boxRepository, org.mockito.Mockito.never()).save(any());
    }
}
