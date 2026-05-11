package com.telas.services.impl;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.entities.Client;
import com.telas.helpers.AttachmentHelper;
import com.telas.helpers.ClientHelper;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.enums.NotificationReference;
import com.telas.repositories.AdMessageRepository;
import com.telas.repositories.AdRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.MonitorAdRepository;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.services.BucketService;
import com.telas.services.ClientPermanentDeletionService;
import com.telas.services.NotificationService;
import com.telas.services.PermissionService;
import com.telas.services.TermConditionService;
import com.telas.services.VerificationCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplUploadAttachmentsNotifyAdminTest {

    @Mock
    private ClientRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
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
    @Mock
    private NotificationService notificationService;
    @Mock
    private AdRepository adRepository;

    @InjectMocks
    private ClientServiceImpl service;

    @Test
    void uploadAttachments_whenFirstUpload_mustNotifyAdmins() {
        ReflectionTestUtils.setField(service, "frontBaseUrl", "https://front.test");
        Client client = new Client();
        client.setAttachments(List.of());

        when(authenticatedUserService.validateActiveSubscription()).thenReturn(new AuthenticatedUser(client));

        AttachmentRequestDto dto = new AttachmentRequestDto();
        dto.setName("test.png");
        dto.setType("image/png");
        dto.setBytes(new byte[0]);

        service.uploadAttachments(List.of(dto));

        verify(attachmentHelper).notifyAdminsClientFirstAttachmentsUploaded(client);
        verify(notificationService).save(
                eq(NotificationReference.CLIENT_FIRST_ATTACHMENTS_UPLOADED_ACK),
                eq(client),
                any(),
                eq(true)
        );
        verify(repository).save(any(Client.class));
    }
}

