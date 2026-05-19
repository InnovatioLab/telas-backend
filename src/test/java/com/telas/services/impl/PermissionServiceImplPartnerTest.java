package com.telas.services.impl;

import com.telas.entities.Client;
import com.telas.entities.ClientGrantedPermission;
import com.telas.enums.Permission;
import com.telas.enums.Role;
import com.telas.repositories.ClientGrantedPermissionRepository;
import com.telas.repositories.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplPartnerTest {

    @Mock
    private ClientGrantedPermissionRepository clientGrantedPermissionRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    void partnerCatalogContainsOnlyPartnerSlotsPermission() {
        List<String> catalog = permissionService.listPermissionCatalogForRole(Role.PARTNER);
        assertEquals(List.of(Permission.PARTNER_SLOTS_ANY_LOCATION.name()), catalog);
    }

    @Test
    void adminCatalogExcludesPartnerOnlyPermission() {
        List<String> catalog = permissionService.listPermissionCatalogForRole(Role.ADMIN);
        assertTrue(catalog.stream().noneMatch(Permission.PARTNER_SLOTS_ANY_LOCATION.name()::equals));
    }

    @Test
    void replacePermissionsForPartnerPersistsGrant() {
        UUID partnerId = UUID.randomUUID();
        UUID devId = UUID.randomUUID();

        Client partner = new Client();
        partner.setId(partnerId);
        partner.setRole(Role.PARTNER);

        Client developer = new Client();
        developer.setId(devId);
        developer.setRole(Role.DEVELOPER);

        when(clientRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        when(clientRepository.findById(devId)).thenReturn(Optional.of(developer));

        permissionService.replacePermissionsForClient(
                partnerId, Set.of(Permission.PARTNER_SLOTS_ANY_LOCATION), devId);

        ArgumentCaptor<ClientGrantedPermission> captor =
                ArgumentCaptor.forClass(ClientGrantedPermission.class);
        verify(clientGrantedPermissionRepository).save(captor.capture());
        assertEquals(
                Permission.PARTNER_SLOTS_ANY_LOCATION.name(),
                captor.getValue().getPermissionCode());
    }

    @Test
    void replacePermissionsForPartnerRejectsAdminPermission() {
        UUID partnerId = UUID.randomUUID();
        UUID devId = UUID.randomUUID();

        Client partner = new Client();
        partner.setId(partnerId);
        partner.setRole(Role.PARTNER);

        Client developer = new Client();
        developer.setId(devId);

        when(clientRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        when(clientRepository.findById(devId)).thenReturn(Optional.of(developer));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        permissionService.replacePermissionsForClient(
                                partnerId, Set.of(Permission.ADMIN_ADS_MANAGE), devId));
    }
}
