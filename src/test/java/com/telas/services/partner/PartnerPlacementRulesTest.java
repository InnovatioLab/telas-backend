package com.telas.services.partner;

import com.telas.entities.Address;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerPlacementRulesTest {

    private PartnerPlacementRules rules;

    @BeforeEach
    void setUp() {
        rules = new PartnerPlacementRules();
    }

    @Test
    void partnerOwnsMonitorAddress_returnsTrueWhenAddressClientMatchesPartner() {
        UUID partnerId = UUID.randomUUID();
        Client partner = partnerClient(partnerId);
        Monitor monitor = monitorOwnedBy(partnerId);

        assertTrue(rules.partnerOwnsMonitorAddress(partner, monitor));
        assertFalse(rules.isForeignPlacementForPartner(partner, monitor));
        assertTrue(rules.partnerOwnsMonitor(monitor, partner));
    }

    @Test
    void isForeignPlacementForPartner_returnsTrueWhenAddressClientDiffers() {
        UUID partnerId = UUID.randomUUID();
        Client partner = partnerClient(partnerId);
        Monitor monitor = monitorOwnedBy(UUID.randomUUID());

        assertFalse(rules.partnerOwnsMonitorAddress(partner, monitor));
        assertTrue(rules.isForeignPlacementForPartner(partner, monitor));
        assertFalse(rules.partnerOwnsMonitor(monitor, partner));
    }

    private static Client partnerClient(UUID id) {
        Client client = new Client();
        client.setId(id);
        client.setRole(Role.PARTNER);
        return client;
    }

    private static Monitor monitorOwnedBy(UUID ownerId) {
        Client owner = new Client();
        owner.setId(ownerId);
        Address address = new Address();
        address.setClient(owner);
        Monitor monitor = new Monitor();
        monitor.setAddress(address);
        return monitor;
    }
}
