package com.telas.services.impl;

import com.telas.dtos.response.ClientResponseDto;
import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.enums.AdValidationType;
import com.telas.helpers.AttachmentHelper;
import com.telas.helpers.ClientHelper;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdMessageRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.MonitorAdRepository;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.services.BucketService;
import com.telas.services.ClientPermanentDeletionService;
import com.telas.services.PermissionService;
import com.telas.services.TermConditionService;
import com.telas.services.VerificationCodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplAdsSoftHideTest {

    @Mock
    private ClientRepository repository;
    @Mock
    private ClientHelper helper;
    @Mock
    private AttachmentHelper attachmentHelper;
    @Mock
    private VerificationCodeService verificationCodeService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private BucketService bucketService;
    @Mock
    private TermConditionService termConditionService;
    @Mock
    private AdRequestRepository adRequestRepository;
    @Mock
    private AdMessageRepository adMessageRepository;
    @Mock
    private MonitorAdRepository monitorAdRepository;
    @Mock
    private PermissionService permissionService;
    @Mock
    private AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;
    @Mock
    private ClientPermanentDeletionService clientPermanentDeletionService;

    @InjectMocks
    private ClientServiceImpl service;

    @Test
    void buildClientResponse_mustNotExposeRejectedAdsToClient() {
        Client client = new Client();
        client.setId(UUID.randomUUID());

        Ad approved = new Ad();
        approved.setId(UUID.randomUUID());
        approved.setClient(client);
        approved.setName("approved.png");
        approved.setCreatedAt(Instant.now());
        approved.setValidation(AdValidationType.APPROVED);

        Ad rejected = new Ad();
        rejected.setId(UUID.randomUUID());
        rejected.setClient(client);
        rejected.setName("rejected.png");
        rejected.setCreatedAt(Instant.now());
        rejected.setValidation(AdValidationType.REJECTED);

        client.setAds(List.of(approved, rejected));

        when(attachmentHelper.getStringLinkFromAd(approved)).thenReturn("link-approved");
        when(attachmentHelper.getDownloadLinkFromAd(approved)).thenReturn("dl-approved");
        when(permissionService.listEffectivePermissionCodesForDisplay(client)).thenReturn(List.of());

        ClientResponseDto dto = service.buildClientResponse(client);

        assertThat(dto.getAds())
                .extracting(a -> a.getValidation())
                .doesNotContain(AdValidationType.REJECTED);
    }
}

