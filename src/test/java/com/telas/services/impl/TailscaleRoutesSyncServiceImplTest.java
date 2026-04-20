package com.telas.services.impl;

import com.telas.monitoring.repositories.BoxSubnetRouteEntityRepository;
import com.telas.repositories.BoxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TailscaleRoutesSyncServiceImplTest {

    @Mock private TailscaleAccessTokenProvider tokenProvider;
    @Mock private BoxRepository boxRepository;
    @Mock private BoxSubnetRouteEntityRepository boxSubnetRouteEntityRepository;

    @Test
    void syncSubnetRoutes_skipsWhenDisabled() {
        TailscaleRoutesSyncServiceImpl service =
                new TailscaleRoutesSyncServiceImpl(tokenProvider, boxRepository, boxSubnetRouteEntityRepository);
        ReflectionTestUtils.setField(service, "enabled", false);
        Map<String, Object> out = service.syncSubnetRoutes();
        assertThat(out.get("tailscaleSyncAttempted")).isEqualTo(false);
        assertThat(out.get("tailscaleSyncSkipped")).isEqualTo("disabled");
    }
}
