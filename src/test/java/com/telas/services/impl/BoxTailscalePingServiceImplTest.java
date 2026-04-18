package com.telas.services.impl;

import com.telas.services.BoxTailscalePingOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BoxTailscalePingServiceImplTest {

    private final BoxTailscalePingServiceImpl service = new BoxTailscalePingServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pingEnabled", true);
        ReflectionTestUtils.setField(service, "timeoutSeconds", 3);
    }

    @Test
    void ping_disabled_returns_not_attempted() {
        ReflectionTestUtils.setField(service, "pingEnabled", false);
        BoxTailscalePingOutcome o = service.pingBoxAddressIp("100.64.0.1");
        assertThat(o.attempted()).isFalse();
        assertThat(o.detail()).isEqualTo("ping_disabled");
    }

    @Test
    void ping_blank_ip_returns_not_attempted() {
        BoxTailscalePingOutcome o = service.pingBoxAddressIp("  ");
        assertThat(o.attempted()).isFalse();
        assertThat(o.detail()).isEqualTo("no_ip");
    }

    @Test
    void ping_non_ipv4_returns_attempted_false() {
        BoxTailscalePingOutcome o = service.pingBoxAddressIp("not-an-ip");
        assertThat(o.attempted()).isTrue();
        assertThat(o.reachable()).isFalse();
        assertThat(o.detail()).contains("ipv4");
    }
}
