package com.telas.helpers;

import com.telas.dtos.request.RefusedAdRequestDto;
import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.enums.AdValidationType;
import com.telas.repositories.AdRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.AttachmentRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.AdUnusedTrackingService;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.services.BucketService;
import com.telas.services.NotificationService;
import com.telas.services.PermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentHelperValidateApprovedNotificationTest {

    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private AdRepository adRepository;
    @Mock
    private BucketService bucketService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AdRequestRepository adRequestRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private SubscriptionHelper subscriptionHelper;
    @Mock
    private MonitorHelper monitorHelper;
    @Mock
    private PermissionService permissionService;
    @Mock
    private AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;
    @Mock
    private AdUnusedTrackingService adUnusedTrackingService;

    @InjectMocks
    private AttachmentHelper helper;

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
        when(clientRepository.findAllAdminsAndDevelopers()).thenReturn(List.of());

        helper.validateAd(ad, owner, AdValidationType.APPROVED, (RefusedAdRequestDto) null);

        verify(notificationService).save(any(), any(), any(), anyBoolean());
    }
}

