package com.telas.services;

import com.telas.entities.Box;
import com.telas.monitoring.crypto.AesTextEncryptionService;
import com.telas.monitoring.entities.BoxHeartbeatEntity;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.PlugReading;
import com.telas.monitoring.plug.SmartPlugClient;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.monitoring.repositories.CheckRunEntityRepository;
import com.telas.monitoring.repositories.IncidentEntityRepository;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import com.telas.monitoring.state.SmartPlugThresholdState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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
class SmartPlugCheckServiceTest {

    @Mock private SmartPlugEntityRepository smartPlugEntityRepository;
    @Mock private SmartPlugClient smartPlugClient;
    @Mock private AesTextEncryptionService encryptionService;
    @Mock private BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;
    @Mock private IncidentEntityRepository incidentEntityRepository;
    @Mock private CheckRunEntityRepository checkRunEntityRepository;
    @Mock private SmartPlugThresholdState thresholdState;

    private SmartPlugCheckService service;

    private final UUID boxUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SmartPlugCheckService(
                smartPlugEntityRepository,
                smartPlugClient,
                encryptionService,
                boxHeartbeatEntityRepository,
                incidentEntityRepository,
                checkRunEntityRepository,
                thresholdState);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "raiseIncidents", true);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "powerBelowWatts", 5.0);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "minReadingsBelow", 3);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "staleSeconds", 180L);
    }

    @Test
    void runAllChecks_includesBoxOnlyPlug() {
        SmartPlugEntity plug = new SmartPlugEntity();
        plug.setId(UUID.randomUUID());
        Box box = new Box();
        box.setId(boxUuid);
        plug.setBox(box);
        plug.setEnabled(true);

        when(smartPlugEntityRepository.findAllEnabledForChecks()).thenReturn(List.of(plug));
        when(encryptionService.isConfigured()).thenReturn(false);
        when(boxHeartbeatEntityRepository.findByBox_Id(boxUuid))
                .thenReturn(Optional.empty());
        when(smartPlugClient.read(any(), any()))
                .thenReturn(
                        new PlugReading(true, true, 100.0, 120.0, 0.5, null));

        service.runAllChecks();

        verify(checkRunEntityRepository).save(any());
        verify(incidentEntityRepository, never()).save(any());
    }

    @Test
    void maybeCreateIncident_usesBoxWhenPlugIsBoxOnly() {
        SmartPlugEntity plug = new SmartPlugEntity();
        plug.setId(UUID.randomUUID());
        Box box = new Box();
        box.setId(boxUuid);
        plug.setBox(box);

        BoxHeartbeatEntity hb = new BoxHeartbeatEntity();
        hb.setBox(box);
        hb.setLastSeenAt(Instant.now());

        when(encryptionService.isConfigured()).thenReturn(false);
        when(boxHeartbeatEntityRepository.findByBox_Id(boxUuid)).thenReturn(Optional.of(hb));
        when(smartPlugClient.read(any(), any()))
                .thenReturn(new PlugReading(false, null, null, null, null, "timeout"));
        when(incidentEntityRepository.existsByBox_IdAndIncidentTypeAndClosedAtIsNull(
                        eq(boxUuid), eq("OTHER")))
                .thenReturn(false);

        when(smartPlugEntityRepository.findAllEnabledForChecks()).thenReturn(List.of(plug));

        service.runAllChecks();

        ArgumentCaptor<com.telas.monitoring.entities.IncidentEntity> captor =
                ArgumentCaptor.forClass(com.telas.monitoring.entities.IncidentEntity.class);
        verify(incidentEntityRepository).save(captor.capture());
        assertThat(captor.getValue().getMonitor()).isNull();
        assertThat(captor.getValue().getBox()).isEqualTo(box);
    }
}
