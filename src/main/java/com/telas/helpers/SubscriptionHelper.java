package com.telas.helpers;

import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.dtos.response.SubscriptionMonitorResponseDto;
import com.telas.dtos.response.SubscriptionResponseDto;
import com.telas.entities.*;
import com.telas.enums.NotificationReference;
import com.telas.enums.Recurrence;
import com.telas.enums.SubscriptionStatus;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.repositories.SubscriptionFlowRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.*;
import com.telas.services.payment.StripeSubscriptionLifecycle;
import com.telas.services.payment.SubscriptionPurchaseCompletionHandler;
import com.telas.services.partner.PartnerPlacementRules;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.CartValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.constants.valitation.SubscriptionValidationMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SubscriptionHelper {
    private final Logger log = LoggerFactory.getLogger(SubscriptionHelper.class);
    private final SubscriptionRepository repository;
    private final SubscriptionFlowRepository subscriptionFlowRepository;
    private final CartService cartService;
    private final MonitorRepository monitorRepository;
    private final MonitorSubscriptionService monitorSubscriptionService;
    private final AdMediaLinkFactory adMediaLinkFactory;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final ClientHelper clientHelper;
    private final StripeSubscriptionLifecycle stripeSubscriptionLifecycle;
    private final SubscriptionPurchaseCompletionHandler subscriptionPurchaseCompletionHandler;
    private final PartnerSlotAccessService partnerSlotAccessService;
    private final PartnerPlacementRules partnerPlacementRules;

    public SubscriptionHelper(
            SubscriptionRepository repository,
            SubscriptionFlowRepository subscriptionFlowRepository,
            @Lazy CartService cartService,
            MonitorRepository monitorRepository,
            MonitorSubscriptionService monitorSubscriptionService,
            AdMediaLinkFactory adMediaLinkFactory,
            NotificationService notificationService,
            PaymentService paymentService,
            ClientHelper clientHelper,
            StripeSubscriptionLifecycle stripeSubscriptionLifecycle,
            SubscriptionPurchaseCompletionHandler subscriptionPurchaseCompletionHandler,
            PartnerSlotAccessService partnerSlotAccessService,
            PartnerPlacementRules partnerPlacementRules
    ) {
        this.repository = repository;
        this.subscriptionFlowRepository = subscriptionFlowRepository;
        this.cartService = cartService;
        this.monitorRepository = monitorRepository;
        this.monitorSubscriptionService = monitorSubscriptionService;
        this.adMediaLinkFactory = adMediaLinkFactory;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
        this.clientHelper = clientHelper;
        this.stripeSubscriptionLifecycle = stripeSubscriptionLifecycle;
        this.subscriptionPurchaseCompletionHandler = subscriptionPurchaseCompletionHandler;
        this.partnerSlotAccessService = partnerSlotAccessService;
        this.partnerPlacementRules = partnerPlacementRules;
    }

    @Transactional
    public Cart getAndValidateActiveCart(Client client) {
        Cart cart = cartService.findActiveByClientIdWithItens(client.getId());

        validateCart(cart);
        validateItems(cart.getItems());
        return cart;
    }

    @Transactional
    public void deleteSubscriptionFlow(Client client) {
        Optional.ofNullable(client.getSubscriptionFlow())
                .ifPresent(subscriptionFlow -> {
                    subscriptionFlowRepository.deleteById(subscriptionFlow.getId());
                    client.setSubscriptionFlow(null);
                });
    }

    @Transactional
    public void inactivateCart(Client client) {
        try {
            Cart cart = cartService.findActiveByClientIdWithItens(client.getId());
            cartService.inactivateCart(cart);
        } catch (ResourceNotFoundException ignored) {
        }
    }

    @Transactional
    public RemoveMonitorAdsOutcome removeMonitorAdsFromSubscription(Subscription subscription) {
        return monitorSubscriptionService.removeMonitorAdsFromSubscription(subscription);
    }

    @Transactional
    public Subscription findEntityById(UUID subscriptionId) {
        return repository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException(SubscriptionValidationMessages.SUBSCRIPTION_NOT_FOUND));
    }

    @Transactional
    public void setAuditInfo(Subscription subscription, String agent) {
        CustomRevisionListener.setUsername(agent);
        subscription.setUsernameUpdate(agent);
    }

    @Transactional
    public com.stripe.model.Subscription getStripeSubscription(Subscription subscription) throws StripeException {
        return stripeSubscriptionLifecycle.retrieveActiveStripeSubscription(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponseDto getSubscriptionResponse(Subscription subscription, Client loggedUser) {
        SubscriptionResponseDto response = new SubscriptionResponseDto(subscription);
        List<Monitor> monitors = monitorRepository.findAllByIdIn(response.getMonitors().stream().map(SubscriptionMonitorResponseDto::getId).toList());

        if (monitors.isEmpty()) {
            return response;
        }

        Map<UUID, List<MonitorAdResponseDto>> monitorAdLinksMap = monitors.stream()
                .collect(Collectors.toMap(
                        Monitor::getId,
                        monitor -> monitor.getMonitorAds().stream()
                                .filter(monitorAd -> loggedUser.isAdmin() || monitorAd.getAd().getClient().getId().equals(loggedUser.getId()))
                                .map(monitorAd -> new MonitorAdResponseDto(monitorAd, adMediaLinkFactory.getLink(monitorAd.getAd())))
                                .toList()
                ));

        response.getMonitors().forEach(monitor ->
                monitor.setAdLinks(monitorAdLinksMap.getOrDefault(monitor.getId(), List.of()))
        );

        return response;
    }

    @Transactional
    public void handleNonRecurringPayment(Subscription subscription) {
        subscriptionPurchaseCompletionHandler.handleNonRecurringPayment(subscription);
    }

    @Transactional
    public void handleBonusSubscription(Subscription subscription) {
    }

    @Transactional
    public void validateSubscriptionForUpgrade(Subscription entity, Recurrence recurrence) {
        if (entity.isBonus()) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_BONUS);
        }

        if (entity.isUpgrade()) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_ALREADY_ON_UPGRADE);
        }

        entity.getRecurrence().validateUpgradeTo(recurrence);

        if (isInactiveOrExpired(entity)) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_NON_ACTIVE_OR_EXPIRED);
        }

        if (Recurrence.MONTHLY.equals(recurrence) && hasExcessiveRemainingTime(entity)) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_SHORT_BILLING_CYCLE);
        }
    }

    @Transactional
    public void validateSubscriptionForRenewal(Subscription entity, Client client) {
        if (entity.isBonus()) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_RENEW_NOT_ALLOWED_FOR_BONUS);
        }

        if (Recurrence.MONTHLY.equals(entity.getRecurrence())) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_RENEW_NOT_ALLOWED_FOR_MONTHLY);
        }

        if (isInactiveOrExpired(entity)) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_NON_ACTIVE_OR_EXPIRED);
        }

        if (entity.isUpgrade()) {
            entity.setUpgrade(false);
        }
    }

    private boolean isInactiveOrExpired(Subscription entity) {
        boolean isExpired = entity.getEndsAt() != null && !entity.getEndsAt().isAfter(Instant.now());
        boolean isInactive = !SubscriptionStatus.ACTIVE.equals(entity.getStatus());
        return isInactive || isExpired;
    }

    private boolean hasExcessiveRemainingTime(Subscription entity) {
        if (entity.getEndsAt() == null) {
            return false;
        }
        long remainingTime = entity.getEndsAt().getEpochSecond() - Instant.now().getEpochSecond();
        return remainingTime > SharedConstants.MAX_BILLING_CYCLE_ANCHOR;
    }

    public void sendPurchaseConfirmationEmail(Subscription subscription) {
        subscriptionPurchaseCompletionHandler.sendPurchaseConfirmationEmail(subscription);
    }

    @Transactional
    @Async
    public void notifyClientsWishList(List<Client> clients, Set<Monitor> monitors) {
        clients.forEach(client -> {
            Set<Monitor> wishlistMonitors = new HashSet<>(client.getWishlist().getMonitors());
            wishlistMonitors.retainAll(monitors);

            if (!wishlistMonitors.isEmpty()) {
                log.info("Notifying client {} about available monitors in wishlist", client.getBusinessName());

                Map<String, String> params = Map.of(
                        "clientName", client.getBusinessName(),
                        "name", client.getBusinessName(),
                        "monitorsAddress", wishlistMonitors.stream()
                                .map(m -> m.getAddress().getCoordinatesParams())
                                .collect(Collectors.joining(", ")),
                        "link", "/client/wishlist"
                );

                notificationService.save(NotificationReference.MONITOR_IN_WISHLIST_NOW_AVAILABLE, client, params, true);
            }
        });
    }

    public String getRedirectUrlAfterCreatingNewSubscription() {
        return subscriptionPurchaseCompletionHandler.redirectUrlAfterCreatingNewSubscription();
    }

    private void validateCart(Cart cart) {
        if (cart.getItems().isEmpty()) {
            throw new BusinessRuleException(CartValidationMessages.CART_EMPTY);
        }

        if (!cart.isActive()) {
            throw new BusinessRuleException(CartValidationMessages.CART_INACTIVE);
        }
    }

    private void validateItems(List<CartItem> items) {
        Client client = items.get(0).getCart().getClient();

        // Sort IDs before locking to ensure consistent lock ordering across transactions (prevents deadlocks).
        List<UUID> monitorIds = items.stream()
                .map(CartItem::getMonitor)
                .filter(Objects::nonNull)
                .filter(monitor -> !partnerPlacementRules.partnerOwnsMonitor(monitor, client))
                .map(Monitor::getId)
                .sorted()
                .toList();

        Map<UUID, Monitor> monitors = monitorRepository.findAllByIdInForUpdate(monitorIds)
                .stream().collect(Collectors.toMap(Monitor::getId, monitor -> monitor));

        items.removeIf(item -> !monitors.containsKey(item.getMonitor().getId()));

        List<Monitor> clientActiveMonitors = clientHelper.findClientMonitorsWithActiveSubscriptions(client.getId());

        for (CartItem item : items) {
            Monitor monitor = monitors.get(item.getMonitor().getId());

            if (!partnerSlotAccessService.canAddBlocks(client, monitor, item.getBlockQuantity())) {
                throw new BusinessRuleException(MonitorValidationMessages.MONITOR_BLOCKS_UNAVAILABLE);
            }

            if (!clientActiveMonitors.isEmpty() && clientActiveMonitors.contains(monitor)) {
                throw new BusinessRuleException(SubscriptionValidationMessages.CLIENT_ALREADY_HAS_ACTIVE_SUBSCRIPTION_WITH_MONITOR);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Subscription> getClientActiveSubscriptions(UUID id) {
        return repository.findActiveSubscriptionsByClientId(id);
    }

    @Transactional
    public void voidLatestInvoice(com.stripe.model.Subscription stripeSubscription) {
        try {
            Invoice invoice = stripeSubscription.getLatestInvoiceObject() != null
                    ? stripeSubscription.getLatestInvoiceObject()
                    : Invoice.retrieve(stripeSubscription.getLatestInvoice());

            if (invoice == null) {
                log.warn("No invoices found to void.");
                return;
            }

            if ("paid".equalsIgnoreCase(invoice.getStatus())) {
                log.info("Skipping void for paid invoice {}.", invoice.getId());
                return;
            }

            invoice.voidInvoice();
            log.info("Invoice {} voided successfully.", invoice.getId());
        } catch (StripeException e) {
            log.error("Error when voiding the last invoice: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public String generateCustomerPortalSession(Client client) throws StripeException {
        return stripeSubscriptionLifecycle.createCustomerPortalSession(client, "/client/subscriptions");
    }

    @Transactional
    public String process(Subscription subscription, Recurrence recurrence) {
        return paymentService.process(subscription, recurrence);
    }
}
