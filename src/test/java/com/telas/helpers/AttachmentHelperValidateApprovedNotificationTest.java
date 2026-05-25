package com.telas.helpers;

import com.telas.dtos.request.RefusedAdRequestDto;
import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.enums.AdValidationType;
import com.telas.repositories.AdRepository;
import com.telas.repositories.MonitorAdRepository;
import com.telas.services.AdUnusedTrackingService;
import com.telas.services.NotificationService;
import com.telas.services.ad.AdValidationService;
import com.telas.services.notification.AdminAdsNotificationService;
import com.telas.shared.utils.ClientPortalLinkResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentHelperValidateApprovedNotificationTest {

    @Mock
    private AdRepository adRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AdminAdsNotificationService adminAdsNotificationService;
    @Mock
    private ClientPortalLinkResolver clientPortalLinkResolver;
    @Mock
    private AdUnusedTrackingService adUnusedTrackingService;
    @Mock
    private MonitorAdRepository monitorAdRepository;

    @InjectMocks
    private AdValidationService adValidationService;

    @Test
    void validateAd_whenApproved_mustNotifySomeone() {
        Client owner = new Client();
        owner.setId(UUID.randomUUID());

        Ad ad = new Ad();
        ad.setId(UUID.randomUUID());
        ad.setClient(owner);
        ad.setName("ad.png");
        ad.setValidation(AdValidationType.PENDING);

        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));
        when(monitorAdRepository.findByAdIdWithMonitor(any())).thenReturn(List.of());

        adValidationService.validateAd(ad, owner, AdValidationType.APPROVED, (RefusedAdRequestDto) null);

        verify(notificationService).save(any(), any(), any(), anyBoolean());
    }
}
