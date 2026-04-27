package com.telas.services.impl;

import com.telas.entities.Box;
import com.telas.monitoring.entities.BoxSubnetRouteEntity;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.PlugReading;
import com.telas.monitoring.plug.SmartPlugClient;
import com.telas.monitoring.plug.SmartPlugCredentials;
import com.telas.monitoring.repositories.BoxSubnetRouteEntityRepository;
import com.telas.monitoring.repositories.SmartPlugCheckRunRepository;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import com.telas.services.ApplicationLogService;
import com.telas.services.SmartPlugCredentialsResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartPlugIpDiscoveryServiceImplTest {

    @Mock private SmartPlugEntityRepository smartPlugEntityRepository;
    @Mock private BoxSubnetRouteEntityRepository boxSubnetRouteEntityRepository;
    @Mock private SmartPlugCheckRunRepository smartPlugCheckRunRepository;
    @Mock private SmartPlugClient smartPlugClient;
    @Mock private SmartPlugCredentialsResolver credentialsResolver;
    @Mock private SmartPlugIpDiscoveryPersistence smartPlugIpDiscoveryPersistence;
    @Mock private ApplicationLogService applicationLogService;

    private ExecutorService executor;

    private SmartPlugIpDiscoveryServiceImpl service;

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
        service =
                new SmartPlugIpDiscoveryServiceImpl(
                        smartPlugEntityRepository,
                        boxSubnetRouteEntityRepository,
                        smartPlugCheckRunRepository,
                        smartPlugClient,
                        credentialsResolver,
                        smartPlugIpDiscoveryPersistence,
                        executor,
                        applicationLogService);
        ReflectionTestUtils.setField(service, "discoveryEnabled", true);
        ReflectionTestUtils.setField(service, "maxParallel", 20);
        ReflectionTestUtils.setField(service, "perPlugMaxAttempts", 500);
    }

    @Test
    void runDiscoveryCycle_persistsIpWhenProbeSucceeds() {
        UUID plugId = UUID.randomUUID();
        UUID boxId = UUID.randomUUID();

        Box box = new Box();
        box.setId(boxId);

        SmartPlugEntity plug = new SmartPlugEntity();
        plug.setId(plugId);
        plug.setBox(box);

        BoxSubnetRouteEntity route = new BoxSubnetRouteEntity();
        route.setCidr("10.0.0.0/24");

        when(smartPlugEntityRepository.findAllEnabledForChecks()).thenReturn(List.of(plug));
        when(smartPlugCheckRunRepository.findLastReadingsForAllPlugs()).thenReturn(List.of());
        when(smartPlugEntityRepository.findById(plugId)).thenReturn(Optional.of(plug));
        when(boxSubnetRouteEntityRepository.findByBox_IdOrderByCidrAsc(boxId)).thenReturn(List.of(route));
        when(credentialsResolver.resolve(plug)).thenReturn(new SmartPlugCredentials("u", "p"));

        when(smartPlugClient.readAtHost(eq(plug), any(), any()))
                .thenAnswer(
                        inv -> {
                            String ip = inv.getArgument(1);
                            if ("10.0.0.77".equals(ip)) {
                                return new PlugReading(true, true, 1.0, 120.0, 0.1, null);
                            }
                            return PlugReading.unreachable("stub");
                        });

        service.runDiscoveryCycle();

        ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
        verify(smartPlugIpDiscoveryPersistence).updateLastSeenIp(eq(plugId), ipCaptor.capture());
        assertThat(ipCaptor.getValue()).isEqualTo("10.0.0.77");
    }
}
