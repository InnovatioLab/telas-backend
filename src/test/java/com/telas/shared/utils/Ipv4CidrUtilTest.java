package com.telas.shared.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Ipv4CidrUtilTest {

    @Test
    void filterIpv4Slash24_keepsOrderAndDeduplicates() {
        List<String> out =
                Ipv4CidrUtil.filterIpv4Slash24Cidrs(
                        List.of("192.168.1.0/24", "10.0.0.0/23", "fd00::/64", "10.0.1.0/24"));
        assertThat(out).containsExactly("192.168.1.0/24", "10.0.1.0/24");
    }

    @Test
    void listHostIpsInSlash24_returns254Hosts() {
        List<String> hosts = Ipv4CidrUtil.listHostIpsInSlash24("192.168.1.0/24");
        assertThat(hosts).hasSize(254);
        assertThat(hosts.get(0)).isEqualTo("192.168.1.1");
        assertThat(hosts.get(253)).isEqualTo("192.168.1.254");
    }
}
