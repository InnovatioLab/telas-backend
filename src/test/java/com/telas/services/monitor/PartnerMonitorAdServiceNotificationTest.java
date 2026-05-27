package com.telas.services.monitor;

import com.telas.dtos.request.PartnerAdRequestToAdminDto;
import com.telas.dtos.request.PartnerAdSubmissionRequestDto;
import com.telas.entities.AdRequest;
import com.telas.entities.Address;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.enums.NotificationReference;
import com.telas.enums.PartnerSubmissionMode;
import com.telas.enums.Role;
import com.telas.helpers.ClientHelper;
import com.telas.helpers.MonitorHelper;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdRepository;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.MonitorAdRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.BucketService;
import com.telas.services.NotificationService;
import com.telas.services.PartnerSlotAccessService;
import com.telas.services.ad.AdValidationService;
import com.telas.services.notification.AdminAdsNotificationService;
import com.telas.services.partner.PartnerPlacementRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerMonitorAdServiceNotificationTest {

    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private MonitorRepository monitorRepository;
    @Mock
    private AdRepository adRepository;
    @Mock
    private MonitorAdRepository monitorAdRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private BucketService bucketService;
    @Mock
    private MonitorHelper helper;
    @Mock
    private PartnerSlotAccessService partnerSlotAccessService;
    @Mock
    private PartnerPlacementRules partnerPlacementRules;
    @Mock
    private AdValidationService adValidationService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AdminAdsNotificationService adminAdsNotificationService;
    @Mock
    private ClientHelper clientHelper;
    @Mock
    private MonitorCrudService monitorCrudService;

    @InjectMocks
    private PartnerMonitorAdService service;

    private Client partner;
    private Monitor monitor;
    private UUID monitorId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "frontBaseUrl", "https://front.test");
        monitorId = UUID.randomUUID();
        partner = new Client();
        partner.setId(UUID.randomUUID());
        partner.setBusinessName("Partner Co");
        partner.setRole(Role.PARTNER);
        partner.setAds(List.of());

        monitor = new Monitor();
        monitor.setId(monitorId);
        Address address = new Address();
        address.setStreet("Main St");
        monitor.setAddress(address);

        when(authenticatedUserService.validatePartner()).thenReturn(new AuthenticatedUser(partner));
        when(monitorCrudService.findEntityById(monitorId)).thenReturn(monitor);
        when(partnerPlacementRules.partnerOwnsMonitorAddress(partner, monitor)).thenReturn(false);
        when(partnerSlotAccessService.hasGlobalSlotsPermission(partner)).thenReturn(true);
    }

    @Test
    void submitPartnerAdSubmission_createAd_notifiesAdminAndPartner() {
        UUID attachmentId = UUID.randomUUID();
        PartnerAdSubmissionRequestDto request = new PartnerAdSubmissionRequestDto();
        request.setSubmissionMode(PartnerSubmissionMode.ADMIN_MATERIALS);
        request.setAttachmentIds(List.of(attachmentId));
        request.setOptionalLabel("Use blue branding");

        AdRequest created = new AdRequest();
        created.setId(UUID.randomUUID());
        created.setSlogan("Use blue branding");
        when(clientHelper.createPartnerAdRequest(any(PartnerAdRequestToAdminDto.class), eq(partner), eq(monitor)))
                .thenReturn(created);

        service.submitPartnerAdSubmission(monitorId, request);

        ArgumentCaptor<Map<String, String>> adminParams = ArgumentCaptor.forClass(Map.class);
        verify(adminAdsNotificationService).notifyAdmins(
                eq(NotificationReference.ADMIN_PARTNER_PLACEMENT_REQUEST),
                adminParams.capture());
        assertThat(adminParams.getValue().get("instructions")).isEqualTo("Use blue branding");
        assertThat(adminParams.getValue().get("partnerName")).isEqualTo("Partner Co");

        ArgumentCaptor<Map<String, String>> partnerParams = ArgumentCaptor.forClass(Map.class);
        verify(notificationService).save(
                eq(NotificationReference.CLIENT_PARTNER_SUBMISSION_ACK),
                eq(partner),
                partnerParams.capture(),
                eq(true));
        assertThat(partnerParams.getValue().get("submissionType")).isEqualTo("Create Ad");
    }

    @Test
    void submitPartnerAdSubmission_finishedAd_notifiesAdminWithFinishedRef() {
        PartnerAdSubmissionRequestDto request = new PartnerAdSubmissionRequestDto();
        request.setSubmissionMode(PartnerSubmissionMode.PARTNER_FINISHED_CREATIVE);
        com.telas.dtos.request.AttachmentRequestDto attachment = new com.telas.dtos.request.AttachmentRequestDto();
        attachment.setName("ad.png");
        attachment.setType("image/png");
        attachment.setBytes(new byte[]{1});
        request.setAttachment(attachment);

        AdRequest created = new AdRequest();
        created.setId(UUID.randomUUID());
        when(clientHelper.createPartnerFinishedCreativeRequest(any(), eq(partner), eq(monitor), any()))
                .thenReturn(created);

        service.submitPartnerAdSubmission(monitorId, request);

        verify(adminAdsNotificationService).notifyAdmins(
                eq(NotificationReference.ADMIN_PARTNER_FINISHED_AD_SUBMITTED),
                any());
        verify(notificationService).save(
                eq(NotificationReference.CLIENT_PARTNER_SUBMISSION_ACK),
                eq(partner),
                any(),
                eq(true));
    }
}
