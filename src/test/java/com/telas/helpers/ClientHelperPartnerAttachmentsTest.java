package com.telas.helpers;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.entities.Attachment;
import com.telas.entities.Client;
import com.telas.enums.Role;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.repositories.AdRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.repositories.AttachmentRepository;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.ContactRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.repositories.SubscriptionMonitorRepository;
import com.telas.services.BucketService;
import com.telas.services.NotificationService;
import com.telas.services.PartnerSlotAccessService;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ClientHelperPartnerAttachmentsTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private MonitorRepository monitorRepository;
    @Mock
    private ContactRepository contactRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private AdRepository adRepository;
    @Mock
    private AdRequestRepository adRequestRepository;
    @Mock
    private SubscriptionMonitorRepository subscriptionMonitorRepository;
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

    @InjectMocks
    private ClientHelper clientHelper;

    private AttachmentRequestDto newAttachment;

    @BeforeEach
    void setUp() {
        newAttachment = new AttachmentRequestDto();
        newAttachment.setName("new-file.png");
        newAttachment.setType("image/png");
        newAttachment.setBytes(new byte[]{1});
    }

    @Test
    void validateAttachmentsCount_partnerWithFiveExisting_doesNotThrow() {
        Client partner = new Client();
        partner.setId(UUID.randomUUID());
        partner.setRole(Role.PARTNER);
        List<Attachment> existing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            existing.add(new Attachment());
        }
        partner.setAttachments(existing);

        assertDoesNotThrow(() -> clientHelper.validateAttachmentsCount(partner, List.of(newAttachment)));
    }

    @Test
    void validateAttachmentsCount_customerWithFiveExisting_throws() {
        Client customer = new Client();
        customer.setId(UUID.randomUUID());
        customer.setRole(Role.CLIENT);
        List<Attachment> existing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            existing.add(new Attachment());
        }
        customer.setAttachments(existing);

        assertThrows(BusinessRuleException.class,
                () -> clientHelper.validateAttachmentsCount(customer, List.of(newAttachment)));
    }
}
