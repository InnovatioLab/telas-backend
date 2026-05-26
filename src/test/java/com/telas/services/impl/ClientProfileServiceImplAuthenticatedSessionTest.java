package com.telas.services.impl;

import com.telas.dtos.response.ClientResponseDto;
import com.telas.entities.Client;
import com.telas.enums.DefaultStatus;
import com.telas.enums.Role;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.PermissionService;
import com.telas.services.PartnerPlatformSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientProfileServiceImplAuthenticatedSessionTest {

    @Mock
    private ClientRepository repository;
    @Mock
    private AdRequestRepository adRequestRepository;
    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private PartnerPlatformSettingsService partnerPlatformSettingsService;

    @InjectMocks
    private ClientProfileServiceImpl service;

    @Test
    void getDataFromToken_shouldNotLoadAdsOrAttachments() {
        UUID clientId = UUID.randomUUID();
        Client client = new Client();
        client.setId(clientId);
        client.setRole(Role.CLIENT);
        client.setStatus(DefaultStatus.ACTIVE);

        when(authenticatedUserService.getLoggedUser()).thenReturn(new AuthenticatedUser(client));
        when(repository.findActiveForAuthenticatedSession(clientId)).thenReturn(Optional.of(client));
        when(adRequestRepository.existsByClientId(clientId)).thenReturn(true);
        when(permissionService.listEffectivePermissionCodesForDisplay(client)).thenReturn(List.of());

        ClientResponseDto dto = service.getDataFromToken();

        assertThat(dto.getAds()).isEmpty();
        assertThat(dto.getAttachments()).isEmpty();
        assertThat(dto.getAdRequest()).isNull();
        assertThat(dto.isHasAdRequest()).isTrue();
        verify(repository, never()).findActiveIdFromToken(clientId);
    }
}
