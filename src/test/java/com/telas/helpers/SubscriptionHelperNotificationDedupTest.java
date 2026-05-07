package com.telas.helpers;

import com.telas.entities.Client;
import com.telas.entities.Subscription;
import com.telas.enums.NotificationReference;
import com.telas.enums.Recurrence;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.repositories.SubscriptionFlowRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.BucketService;
import com.telas.services.CartService;
import com.telas.services.EmailService;
import com.telas.services.MonitorSubscriptionService;
import com.telas.services.NotificationService;
import com.telas.services.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionHelperNotificationDedupTest {

    @Mock
    private SubscriptionRepository repository;

    @Mock
    private SubscriptionFlowRepository subscriptionFlowRepository;

    @Mock
    private CartService cartService;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private MonitorSubscriptionService monitorSubscriptionService;

    @Mock
    private BucketService bucketService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ClientHelper clientHelper;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private SubscriptionHelper helper;

    @Test
    void handleNonRecurringPayment_shouldNotCreateDuplicateClientConfirmationNotification() {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setBusinessName("ACME");
        client.setAds(List.of());

        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setClient(client);
        subscription.setStartedAt(Instant.now());
        subscription.setRecurrence(Recurrence.MONTHLY);

        doThrow(new ResourceNotFoundException("cart not found")).when(cartService).findActiveByClientIdWithItens(any());
        when(clientRepository.findAllAdmins()).thenReturn(List.of());

        helper.handleNonRecurringPayment(subscription);

        verify(notificationService, times(1)).save(eq(NotificationReference.FIRST_SUBSCRIPTION), eq(client), any(), eq(true));
        verify(notificationService, times(0)).save(eq(NotificationReference.NEW_SUBSCRIPTION), eq(client), any(), anyBoolean());
    }
}

