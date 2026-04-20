package com.telas.services;

import com.telas.entities.Box;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.PlugReading;
import com.telas.monitoring.plug.SmartPlugClient;
import com.telas.monitoring.plug.SmartPlugCredentials;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.monitoring.repositories.CheckRunEntityRepository;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartPlugCheckServiceTest {

    @Mock private SmartPlugEntityRepository smartPlugEntityRepository;
    @Mock private SmartPlugClient smartPlugClient;
    @Mock private SmartPlugCredentialsResolver credentialsResolver;
    @Mock private BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;
    @Mock private CheckRunEntityRepository checkRunEntityRepository;
    @Mock private SmartPlugRulesService smartPlugRulesService;

    private SmartPlugCheckService service;

    private final UUID boxUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new SmartPlugCheckService(
                        smartPlugEntityRepository,
                        smartPlugClient,
                        credentialsResolver,
                        boxHeartbeatEntityRepository,
                        checkRunEntityRepository,
                        smartPlugRulesService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "raiseIncidents", true);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "staleSeconds", 180L);
    }

    @Test
    void runAllChecks_resolvesCredentials_callsRead_persistsRun_evaluatesRules() {
        SmartPlugEntity plug = new SmartPlugEntity();
        plug.setId(UUID.randomUUID());
        Box box = new Box();
        box.setId(boxUuid);
        plug.setBox(box);
        plug.setEnabled(true);

        when(smartPlugEntityRepository.findAllEnabledForChecks()).thenReturn(List.of(plug));
        when(credentialsResolver.resolve(plug)).thenReturn(new SmartPlugCredentials("u", "p"));
        when(smartPlugClient.read(any(), any()))
                .thenReturn(new PlugReading(true, true, 100.0, 120.0, 0.5, null));
        when(boxHeartbeatEntityRepository.findByBox_Id(boxUuid)).thenReturn(Optional.empty());

        service.runAllChecks();

        verify(credentialsResolver).resolve(plug);
        verify(smartPlugClient).read(eq(plug), any(SmartPlugCredentials.class));
        verify(checkRunEntityRepository).save(any());
        verify(smartPlugRulesService).evaluate(eq(plug), any(PlugReading.class), eq(true));
    }

    @Test
    void runAllChecks_skipsRulesWhenRaiseIncidentsFalse() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "raiseIncidents", false);

        SmartPlugEntity plug = new SmartPlugEntity();
        plug.setId(UUID.randomUUID());
        plug.setBox(new Box());
        plug.getBox().setId(boxUuid);

        when(smartPlugEntityRepository.findAllEnabledForChecks()).thenReturn(List.of(plug));
        when(credentialsResolver.resolve(plug)).thenReturn(null);
        when(smartPlugClient.read(any(), any()))
                .thenReturn(new PlugReading(true, true, 1.0, 120.0, 0.1, null));

        service.runAllChecks();

        verify(checkRunEntityRepository).save(any());
        verify(smartPlugRulesService, never()).evaluate(any(), any(), anyBoolean());
    }
}
