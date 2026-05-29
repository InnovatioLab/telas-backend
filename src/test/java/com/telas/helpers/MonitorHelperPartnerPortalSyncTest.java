package com.telas.helpers;

import com.telas.entities.Ad;
import com.telas.entities.AdRequest;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.enums.Role;
import com.telas.repositories.AdRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.repositories.SubscriptionMonitorRepository;
import com.telas.services.AddressService;
import com.telas.services.MapsService;
import com.telas.services.box.BoxPlaylistClient;
import com.telas.infra.security.services.AuthenticatedUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorHelperPartnerPortalSyncTest {

    @Mock
    private MapsService mapsService;
    @Mock
    private AdRepository adRepository;
    @Mock
    private AdRequestRepository adRequestRepository;
    @Mock
    private MonitorRepository repository;
    @Mock
    private SubscriptionMonitorRepository subscriptionMonitorRepository;
    @Mock
    private AddressService addressService;
    @Mock
    private MonitorAdDtoMapper monitorAdDtoMapper;
    @Mock
    private BoxPlaylistClient boxPlaylistClient;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private MonitorHelper monitorHelper;

    private UUID monitorId;
    private UUID adId;
    private Monitor monitor;
    private Ad ad;
    private AdRequest adRequest;

    @BeforeEach
    void setUp() {
        monitorId = UUID.randomUUID();
        adId = UUID.randomUUID();

        monitor = new Monitor();
        monitor.setId(monitorId);

        Client partner = new Client();
        partner.setId(UUID.randomUUID());
        partner.setRole(Role.PARTNER);

        adRequest = new AdRequest();
        adRequest.setId(UUID.randomUUID());
        adRequest.setTargetMonitor(monitor);

        ad = new Ad();
        ad.setId(adId);
        ad.setClient(partner);
        ad.setAdRequest(adRequest);
    }

    @Test
    void syncPartnerPortalAfterAdsRemovedFromMonitor_clearsTargetMonitorForPartnerAd() {
        when(adRepository.findByIdWithClientAndAdRequest(adId)).thenReturn(Optional.of(ad));

        monitorHelper.syncPartnerPortalAfterAdsRemovedFromMonitor(monitor, Set.of(adId));

        ArgumentCaptor<AdRequest> captor = ArgumentCaptor.forClass(AdRequest.class);
        verify(adRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetMonitor()).isNull();
    }

    @Test
    void syncPartnerPortalAfterAdsRemovedFromMonitor_ignoresEmptyRemovalSet() {
        monitorHelper.syncPartnerPortalAfterAdsRemovedFromMonitor(monitor, Set.of());

        verifyNoInteractions(adRepository, adRequestRepository);
    }
}
