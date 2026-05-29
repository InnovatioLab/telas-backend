package com.telas.helpers;

import com.telas.dtos.request.MonitorAdRequestDto;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.entities.Ad;
import com.telas.enums.AdValidationType;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.entities.Monitor;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.repositories.SubscriptionMonitorRepository;
import com.telas.services.AddressService;
import com.telas.services.MapsService;
import com.telas.services.box.BoxPlaylistClient;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorHelperAvailableAdsEligibilityTest {

    @Mock
    private MapsService mapsService;
    @Mock
    private AdRepository adRepository;
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
    private UUID eligibleAdId;
    private UUID ineligibleAdId;

    @BeforeEach
    void setUp() {
        monitorId = UUID.randomUUID();
        eligibleAdId = UUID.randomUUID();
        ineligibleAdId = UUID.randomUUID();
    }

    @Test
    void getAds_rejectsWhenAdNotEligibleForMonitor() {
        MonitorRequestDto request = monitorRequestWithAds(eligibleAdId, ineligibleAdId);
        when(adRepository.findApprovedEligibleForMonitorByIds(any(), eq(monitorId)))
                .thenReturn(List.of(eligibleAd()));

        assertThatThrownBy(() -> monitorHelper.getAds(request, monitorId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(MonitorValidationMessages.AD_NOT_ABLE_TO_ASSIGN_TO_MONITOR);
    }

    @Test
    void getAds_acceptsAdsReturnedByEligibilityQuery() {
        MonitorRequestDto request = monitorRequestWithAds(eligibleAdId);
        Ad ad = eligibleAd();
        when(adRepository.findApprovedEligibleForMonitorByIds(any(), eq(monitorId)))
                .thenReturn(List.of(ad));

        List<Ad> result = monitorHelper.getAds(request, monitorId);

        assertThat(result).containsExactly(ad);
        verify(adRepository).findApprovedEligibleForMonitorByIds(any(), eq(monitorId));
    }

    @Test
    void getValidAdsForMonitor_delegatesToFilteredRepository() {
        when(adRepository.findAllApprovedNotInMonitorFiltered(monitorId, "search"))
                .thenReturn(List.of(eligibleAd()));
        when(monitorAdDtoMapper.toValidAdDtos(List.of(eligibleAd())))
                .thenReturn(List.of());

        Monitor monitor = new Monitor();
        monitor.setId(monitorId);

        monitorHelper.getValidAdsForMonitor(monitor, "search");

        verify(adRepository).findAllApprovedNotInMonitorFiltered(eq(monitorId), eq("search"));
    }

    private MonitorRequestDto monitorRequestWithAds(UUID... adIds) {
        MonitorRequestDto request = new MonitorRequestDto();
        List<MonitorAdRequestDto> ads = new java.util.ArrayList<>();
        int order = 1;
        for (UUID adId : adIds) {
            MonitorAdRequestDto dto = new MonitorAdRequestDto();
            dto.setId(adId);
            dto.setOrderIndex(order++);
            dto.setBlockQuantity(1);
            ads.add(dto);
        }
        request.setAds(ads);
        return request;
    }

    private Ad eligibleAd() {
        Ad ad = new Ad();
        ad.setId(eligibleAdId);
        ad.setName("partner-ad.png");
        ad.setType("image/png");
        ad.setValidation(AdValidationType.APPROVED);
        return ad;
    }
}
