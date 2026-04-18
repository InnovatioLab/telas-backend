package com.telas.services.impl;

import com.telas.dtos.request.StatusBoxMonitorsRequestDto;
import com.telas.entities.Box;
import com.telas.entities.BoxAddress;
import com.telas.entities.Monitor;
import com.telas.enums.DefaultStatus;
import com.telas.monitoring.entities.IncidentEntity;
import com.telas.monitoring.repositories.IncidentEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.HealthUpdateService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeartbeatRecoveryServiceImplTest {

    @Mock private IncidentEntityRepository incidentEntityRepository;
    @Mock private BoxRepository boxRepository;
    @Mock private MonitorRepository monitorRepository;
    @Mock private HealthUpdateService healthUpdateService;

    @InjectMocks private HeartbeatRecoveryServiceImpl service;

    @Test
    void recover_closesOpenHeartbeatIncidentsAndCallsHealthUpdateWhenInactiveWithIp() {
        UUID boxId = UUID.randomUUID();
        Box input = new Box();
        input.setId(boxId);
        input.setActive(false);

        IncidentEntity inc = new IncidentEntity();
        inc.setIncidentType(MonitoringIncidentTypes.HEARTBEAT_STALE);
        when(incidentEntityRepository.findAllByBox_IdAndIncidentTypeInAndClosedAtIsNull(
                        eq(boxId), eq(MonitoringIncidentTypes.BOX_OUTAGE_INCIDENT_TYPES)))
                .thenReturn(List.of(inc));

        Box managed = new Box();
        managed.setId(boxId);
        managed.setActive(false);
        BoxAddress addr = new BoxAddress();
        addr.setIp("100.64.0.1");
        addr.setMac("aa:bb:cc:dd:ee:ff");
        managed.setBoxAddress(addr);
        Monitor m = new Monitor();
        m.setActive(false);
        List<Monitor> monitors = new ArrayList<>();
        monitors.add(m);
        managed.setMonitors(monitors);

        when(boxRepository.findById(boxId)).thenReturn(Optional.of(managed));

        service.recoverAfterSuccessfulHeartbeat(input);

        ArgumentCaptor<StatusBoxMonitorsRequestDto> dtoCaptor = ArgumentCaptor.forClass(StatusBoxMonitorsRequestDto.class);
        verify(healthUpdateService).applyHealthUpdate(dtoCaptor.capture());
        assertThat(dtoCaptor.getValue().getStatus()).isEqualTo(DefaultStatus.ACTIVE);
        assertThat(dtoCaptor.getValue().getIp()).isEqualTo("100.64.0.1");

        ArgumentCaptor<IncidentEntity> incCaptor = ArgumentCaptor.forClass(IncidentEntity.class);
        verify(incidentEntityRepository).save(incCaptor.capture());
        assertThat(incCaptor.getValue().getClosedAt()).isNotNull();

        verify(monitorRepository, never()).saveAll(any());
        verify(boxRepository, never()).save(any());
    }

    @Test
    void recover_noOpWhenNoOpenIncidentsAndBoxAlreadyActive() {
        UUID boxId = UUID.randomUUID();
        Box input = new Box();
        input.setId(boxId);
        input.setActive(true);
        when(incidentEntityRepository.findAllByBox_IdAndIncidentTypeInAndClosedAtIsNull(eq(boxId), any()))
                .thenReturn(List.of());
        when(boxRepository.findById(boxId)).thenReturn(Optional.of(input));

        service.recoverAfterSuccessfulHeartbeat(input);

        verify(healthUpdateService, never()).applyHealthUpdate(any());
        verify(boxRepository, never()).save(any());
    }
}
