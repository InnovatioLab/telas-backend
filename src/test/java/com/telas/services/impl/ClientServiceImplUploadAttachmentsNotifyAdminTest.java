package com.telas.services.impl;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.entities.Client;
import com.telas.helpers.AttachmentHelper;
import com.telas.helpers.ClientHelper;
import com.telas.infra.security.model.AuthenticatedUser;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplUploadAttachmentsNotifyAdminTest {

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
    void uploadAttachments_whenFirstUpload_mustNotifyAdmins() {
        Client client = new Client();
        client.setAttachments(List.of());

        when(authenticatedUserService.validateActiveSubscription()).thenReturn(new AuthenticatedUser(client));

        AttachmentRequestDto dto = new AttachmentRequestDto();
        dto.setName("test.png");
        dto.setType("image/png");
        dto.setBytes(new byte[0]);

        service.uploadAttachments(List.of(dto));

        verify(attachmentHelper).notifyAdminsClientFirstAttachmentsUploaded(client);
        verify(repository).save(any(Client.class));
    }
}

