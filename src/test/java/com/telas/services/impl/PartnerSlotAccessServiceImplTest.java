package com.telas.services.impl;

import com.telas.entities.Address;
import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.entities.MonitorAdPK;
import com.telas.enums.Permission;
import com.telas.enums.Role;
import com.telas.services.PermissionService;
import com.telas.shared.constants.SharedConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerSlotAccessServiceImplTest {

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PartnerSlotAccessServiceImpl service;

    private Client partner;
    private Client addressOwner;
    private Monitor monitor;

    @BeforeEach
    void setUp() {
        partner = new Client();
        partner.setId(UUID.randomUUID());
        partner.setRole(Role.PARTNER);

        addressOwner = new Client();
        addressOwner.setId(UUID.randomUUID());
        addressOwner.setRole(Role.PARTNER);

        Address address = new Address();
        address.setClient(addressOwner);

        monitor = new Monitor();
        monitor.setId(UUID.randomUUID());
        monitor.setAddress(address);
        monitor.setMaxBlocks(SharedConstants.MAX_MONITOR_ADS);
        monitor.setMonitorAds(new HashSet<>());
        monitor.setSubscriptionMonitors(new HashSet<>());
    }

    @Test
    void usesPartnerQuotaOnOwnMonitorWithoutGlobalPermission() {
        addressOwner.setId(partner.getId());
        when(permissionService.hasPermission(partner, Permission.PARTNER_SLOTS_ANY_LOCATION))
                .thenReturn(false);

        assertTrue(service.usesPartnerQuotaOnMonitor(partner, monitor));
        assertFalse(service.hasGlobalSlotsPermission(partner));
    }

    @Test
    void usesPartnerQuotaOnForeignMonitorWithGlobalPermission() {
        when(permissionService.hasPermission(partner, Permission.PARTNER_SLOTS_ANY_LOCATION))
                .thenReturn(true);

        assertTrue(service.hasGlobalSlotsPermission(partner));
        assertTrue(service.usesPartnerQuotaOnMonitor(partner, monitor));
        assertEquals(5, service.maxBlocksForClientOnMonitor(partner, monitor));
    }

    @Test
    void foreignMonitorWithoutPermissionUsesClientCap() {
        when(permissionService.hasPermission(partner, Permission.PARTNER_SLOTS_ANY_LOCATION))
                .thenReturn(false);

        assertFalse(service.usesPartnerQuotaOnMonitor(partner, monitor));
        assertEquals(1, service.maxBlocksForClientOnMonitor(partner, monitor));
    }

    @Test
    void canAddFiveBlocksOnForeignMonitorWithPermission() {
        when(permissionService.hasPermission(partner, Permission.PARTNER_SLOTS_ANY_LOCATION))
                .thenReturn(true);

        assertTrue(service.canAddBlocks(partner, monitor, 5));
        assertFalse(service.canAddBlocks(partner, monitor, 6));
    }

    @Test
    void resolveCartBlockQuantityReturnsFiveForPermittedPartner() {
        when(permissionService.hasPermission(partner, Permission.PARTNER_SLOTS_ANY_LOCATION))
                .thenReturn(true);

        assertEquals(5, service.resolveCartBlockQuantity(partner, monitor, null));
    }

    @Test
    void usedBlocksCountsPartnerAdsOnMonitor() {
        when(permissionService.hasPermission(partner, Permission.PARTNER_SLOTS_ANY_LOCATION))
                .thenReturn(true);

        Ad ad = new Ad();
        ad.setClient(partner);

        MonitorAd monitorAd = new MonitorAd();
        MonitorAdPK pk = new MonitorAdPK();
        pk.setMonitor(monitor);
        pk.setAd(ad);
        monitorAd.setId(pk);
        monitorAd.setBlockQuantity(3);
        monitorAd.setOrderIndex(1);

        monitor.getMonitorAds().add(monitorAd);

        assertEquals(3, service.usedBlocksByClientOnMonitor(partner, monitor));
        assertTrue(service.canAddBlocks(partner, monitor, 2));
        assertFalse(service.canAddBlocks(partner, monitor, 3));
    }
}
