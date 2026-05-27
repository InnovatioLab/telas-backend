package com.telas.helpers;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.PartnerAdRequestToAdminDto;
import com.telas.entities.Ad;
import com.telas.entities.AdRequest;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.enums.Role;
import com.telas.repositories.AdRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.AttachmentRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.BucketService;
import com.telas.services.NotificationService;
import com.telas.services.PartnerSlotAccessService;
import com.telas.services.ad.AdUploadService;
import com.telas.services.box.BoxPlaylistClient;
import com.telas.services.client.ClientAddressService;
import com.telas.services.payment.StripeSubscriptionLifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientHelperPartnerAdRequestTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private com.telas.repositories.MonitorRepository monitorRepository;
    @Mock
    private com.telas.repositories.ContactRepository contactRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private AdRepository adRepository;
    @Mock
    private AdRequestRepository adRequestRepository;
    @Mock
    private com.telas.repositories.SubscriptionMonitorRepository subscriptionMonitorRepository;
    @Mock
    private ClientAddressService clientAddressService;
    @Mock
    private BucketService bucketService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BoxAdPushNotificationHelper boxAdPushNotificationHelper;
    @Mock
    private PartnerSlotAccessService partnerSlotAccessService;
    @Mock
    private BoxPlaylistClient boxPlaylistClient;
    @Mock
    private StripeSubscriptionLifecycle stripeSubscriptionLifecycle;
    @Mock
    private AdUploadService adUploadService;

    @InjectMocks
    private ClientHelper clientHelper;

    private Client partner;
    private Monitor monitor;

    @BeforeEach
    void setUp() {
        partner = new Client();
        partner.setId(UUID.randomUUID());
        partner.setRole(Role.PARTNER);
        partner.setAds(new ArrayList<>());

        monitor = new Monitor();
        monitor.setId(UUID.randomUUID());
    }

    @Test
    void createPartnerFinishedCreativeRequest_doesNotCheckExistingAdRequest() {
        PartnerAdRequestToAdminDto request = new PartnerAdRequestToAdminDto();
        request.setTargetMonitorId(monitor.getId());

        AttachmentRequestDto attachment = new AttachmentRequestDto();
        attachment.setName("ad.png");
        attachment.setType("image/png");
        attachment.setBytes(new byte[]{1});

        when(adRequestRepository.save(any(AdRequest.class))).thenAnswer(invocation -> {
            AdRequest ar = invocation.getArgument(0);
            ar.setId(UUID.randomUUID());
            return ar;
        });
        when(adRepository.save(any(Ad.class))).thenAnswer(invocation -> {
            Ad ad = invocation.getArgument(0);
            ad.setId(UUID.randomUUID());
            return ad;
        });

        AdRequest result = clientHelper.createPartnerFinishedCreativeRequest(
                request, partner, monitor, attachment);

        assertThat(result.getId()).isNotNull();
        verify(adRequestRepository, never())
                .existsByClientIdAndRequestOriginAndTargetMonitorIdAndIsActiveTrueAndSubmissionMode(
                        any(), any(), any(), any());
    }
}
