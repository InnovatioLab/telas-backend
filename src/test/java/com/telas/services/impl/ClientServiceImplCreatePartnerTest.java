package com.telas.services.impl;

import com.telas.dtos.request.AddressRequestDto;
import com.telas.dtos.request.ContactRequestDto;
import com.telas.dtos.request.CreatePartnerRequestDto;
import com.telas.entities.Client;
import com.telas.entities.Contact;
import com.telas.entities.VerificationCode;
import com.telas.enums.CodeType;
import com.telas.enums.DefaultStatus;
import com.telas.enums.NotificationReference;
import com.telas.enums.Role;
import com.telas.helpers.ClientHelper;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.services.AuthenticatedUserService;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplCreatePartnerTest {

    @Mock
    private ClientRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ClientHelper helper;
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
    void createPartnerByAdmin_mustCreateActivePartnerWithAdminPassword() {
        ReflectionTestUtils.setField(service, "frontBaseUrl", "https://front.test");

        Client admin = new Client();
        admin.setBusinessName("Admin Corp");
        admin.setRole(Role.ADMIN);
        when(authenticatedUserService.validateAdmin()).thenReturn(new AuthenticatedUser(admin));

        VerificationCode verificationCode = new VerificationCode("123456", java.time.Instant.now().plusSeconds(900), CodeType.PASSWORD);
        verificationCode.setValidated(true);
        when(verificationCodeService.savePreValidated(eq(CodeType.PASSWORD), any(Client.class))).thenReturn(verificationCode);
        when(passwordEncoder.encode("PartnerPass1!")).thenReturn("hashed-password");

        when(repository.save(any(Client.class))).thenAnswer(invocation -> {
            Client saved = invocation.getArgument(0);
            saved.setId(java.util.UUID.randomUUID());
            Contact contact = new Contact();
            contact.setEmail("partner@example.com");
            saved.setContact(contact);
            return saved;
        });

        when(repository.findAllAdmins()).thenReturn(List.of(admin));

        CreatePartnerRequestDto request = buildRequest();

        var response = service.createPartnerByAdmin(request);

        assertNotNull(response);
        assertEquals(Role.PARTNER, response.getRole());
        assertEquals(DefaultStatus.ACTIVE, response.getStatus());

        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(repository).save(clientCaptor.capture());
        Client saved = clientCaptor.getValue();
        assertEquals(Role.PARTNER, saved.getRole());
        assertEquals(DefaultStatus.ACTIVE, saved.getStatus());
        assertEquals("hashed-password", saved.getPassword());
        assertTrue(saved.getVerificationCode().isValidated());

        verify(verificationCodeService, never()).send(any());
        verify(passwordEncoder).encode("PartnerPass1!");
        verify(verificationCodeService).savePreValidated(eq(CodeType.PASSWORD), any(Client.class));

        verify(notificationService).save(
                eq(NotificationReference.ADMIN_NEW_CLIENT_REGISTERED),
                eq(admin),
                any(),
                eq(true));
    }

    private static CreatePartnerRequestDto buildRequest() {
        CreatePartnerRequestDto request = new CreatePartnerRequestDto();
        request.setBusinessName("Partner Biz");
        request.setPassword("PartnerPass1!");
        request.setConfirmPassword("PartnerPass1!");

        ContactRequestDto contact = new ContactRequestDto();
        contact.setEmail("partner@example.com");
        contact.setPhone("5551234567");
        request.setContact(contact);

        AddressRequestDto address = new AddressRequestDto();
        address.setStreet("Main St");
        address.setZipCode("12345");
        address.setCity("Miami");
        address.setState("FL");
        address.setCountry("US");
        request.setAddresses(List.of(address));

        return request;
    }
}
