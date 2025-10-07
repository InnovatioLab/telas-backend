package com.telas.helpers;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.billingportal.Session;
import com.stripe.param.billingportal.SessionCreateParams;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.dtos.response.SubscriptionMonitorResponseDto;
import com.telas.dtos.response.SubscriptionResponseDto;
import com.telas.entities.*;
import com.telas.enums.NotificationReference;
import com.telas.enums.Recurrence;
import com.telas.enums.SubscriptionStatus;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.SubscriptionFlowRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.BucketService;
import com.telas.services.CartService;
import com.telas.services.MonitorService;
import com.telas.services.NotificationService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.CartValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.constants.valitation.SubscriptionValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.DateUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SubscriptionHelper {
    private final Logger log = LoggerFactory.getLogger(SubscriptionHelper.class);
    private final SubscriptionRepository repository;
    private final SubscriptionFlowRepository subscriptionFlowRepository;
    private final CartService cartService;
    private final MonitorService monitorService;
    private final BucketService bucketService;
    private final NotificationService notificationService;
    private final ClientHelper clientHelper;

    @Value("${front.base.url}")
    private String frontBaseUrl;

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
        Cart cart = cartService.findActiveByClientIdWithItens(client.getId());
        cartService.inactivateCart(cart);
    }

    @Transactional
    public void removeMonitorAdsFromSubscription(Subscription subscription) {
        monitorService.removeMonitorAdsFromSubscription(subscription);
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
        validateStripeId(subscription.getStripeId());

        com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.retrieve(subscription.getStripeId());
        validateStripeSubscription(stripeSubscription);

        return stripeSubscription;
    }

    @Transactional(readOnly = true)
    public SubscriptionResponseDto getSubscriptionResponse(Subscription subscription, Client loggedUser) {
        SubscriptionResponseDto response = new SubscriptionResponseDto(subscription);
        List<Monitor> monitors = monitorService.findAllByIds(response.getMonitors().stream().map(SubscriptionMonitorResponseDto::getId).toList());

        if (monitors.isEmpty()) {
            return response;
        }

        Map<UUID, List<MonitorAdResponseDto>> monitorAdLinksMap = monitors.stream()
                .collect(Collectors.toMap(
                        Monitor::getId,
                        monitor -> monitor.getMonitorAds().stream()
                                .filter(monitorAd -> loggedUser.isAdmin() || monitorAd.getAd().getClient().getId().equals(loggedUser.getId()))
                                .map(monitorAd -> new MonitorAdResponseDto(monitorAd, bucketService.getLink(AttachmentUtils.format(monitorAd.getAd()))))
                                .toList()
                ));

        response.getMonitors().forEach(monitor ->
                monitor.setAdLinks(monitorAdLinksMap.getOrDefault(monitor.getId(), List.of()))
        );

        return response;
    }

    @Transactional
    public void handleNonRecurringPayment(Subscription subscription) {
        Client client = subscription.getClient();
        inactivateCart(client);
        deleteSubscriptionFlow(client);

        if (client.isFirstSubscription()) {
            sendFirstBuyEmail(subscription);
            return;
        } else if (!client.getAds().isEmpty() && client.getApprovedAd() != null) {
            clientHelper.addAdToMonitor(subscription.getMonitors(), client);
        }

        createNewSubscriptionNotification(subscription);
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


    @Transactional
    public void sendSubscriptionAboutToExpiryEmail(Subscription subscription) {
        Map<String, String> params = new HashMap<>(Map.of(
                "name", subscription.getClient().getBusinessName(),
                "link", buildRedirectUrl("subscriptions/" + subscription.getId()),
                "endDate", DateUtils.formatInstantToString(subscription.getEndsAt())
        ));

        notificationService.save(NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_REMINDER, subscription.getClient(), params, true);
    }

    @Transactional
    public void sendSubscriptionExpiryTodayEmail(Subscription subscription) {
        Map<String, String> params = new HashMap<>(Map.of(
                "name", subscription.getClient().getBusinessName(),
                "link", buildRedirectUrl("subscriptions/" + subscription.getId())
        ));

        notificationService.save(NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_LAST_DAY, subscription.getClient(), params, true);
    }

    public void sendFirstBuyEmail(Subscription subscription) {
        Map<String, String> params = new HashMap<>(Map.of(
                "name", subscription.getClient().getBusinessName(),
                "locations", String.join(". ", subscription.getMonitorAddresses()),
                "startDate", DateUtils.formatInstantToString(subscription.getStartedAt()),
                "link", getRedirectUrlAfterCreatingNewSubscription(subscription.getClient())
        ));

        if (subscription.getEndsAt() != null) {
            params.put("endDate", DateUtils.formatInstantToString(subscription.getEndsAt()));
        }

        notificationService.save(NotificationReference.FIRST_SUBSCRIPTION, subscription.getClient(), params, true);
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
                        "monitorsAddress", wishlistMonitors.stream()
                                .map(m -> m.getAddress().getCoordinatesParams())
                                .collect(Collectors.joining(", ")),
                        "link", buildRedirectUrl("wishlist")
                );

                notificationService.save(NotificationReference.MONITOR_IN_WISHLIST_NOW_AVAILABLE, client, params, false);
            }
        });
    }

    public String getRedirectUrlAfterCreatingNewSubscription(Client client) {
        if (client.getAttachments().isEmpty()) {
            return buildRedirectUrl("my-telas");
        }

        return client.getAds().isEmpty()
                ? buildRedirectUrl("my-telas?ads=true")
                : buildRedirectUrl("subscriptions");
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
        Map<UUID, Monitor> monitors = monitorService.findAllByIds(
                items.stream().map(item -> item.getMonitor().getId()).toList()
        ).stream().collect(Collectors.toMap(Monitor::getId, monitor -> monitor));

        List<Monitor> clientActiveMonitors = clientHelper.findClientMonitorsWithActiveSubscriptions(client.getId());

        for (CartItem item : items) {
            Monitor monitor = monitors.get(item.getMonitor().getId());
            String msg = " with id: " + item.getMonitor().getId();

            if (monitor == null || !monitor.isActive() || !monitor.hasAvailableBlocks(item.getBlockQuantity())) {
                throw new BusinessRuleException(MonitorValidationMessages.MONITOR_INACTIVE_OR_BLOCKS_UNAVAILABLE + msg);
            }

            if (!clientActiveMonitors.isEmpty() && clientActiveMonitors.contains(monitor)) {
                throw new BusinessRuleException(SubscriptionValidationMessages.CLIENT_ALREADY_HAS_ACTIVE_SUBSCRIPTION_WITH_MONITOR + msg);
            }

            if (monitor.getBox() == null || !monitor.getBox().isActive()) {
                throw new BusinessRuleException(MonitorValidationMessages.MONITOR_BOX_NOT_VALID + msg);
            }
        }
    }

    private void validateStripeId(String stripeId) {
        if (ValidateDataUtils.isNullOrEmptyString(stripeId)) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_WITHOUT_STRIPE_ID);
        }
    }

    private void validateStripeSubscription(com.stripe.model.Subscription stripeSubscription) {
        if (stripeSubscription == null) {
            throw new ResourceNotFoundException(SubscriptionValidationMessages.SUBSCRIPTION_NOT_FOUND_IN_STRIPE);
        }

        if (!"active".equals(stripeSubscription.getStatus())) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_NOT_ACTIVE_IN_STRIPE + stripeSubscription.getId());
        }
    }

    private void createNewSubscriptionNotification(Subscription subscription) {
        Map<String, String> params = new HashMap<>(Map.of(
                "locations", String.join(". ", subscription.getMonitorAddresses()),
                "startDate", DateUtils.formatInstantToString(subscription.getStartedAt()),
                "link", "/client/subscriptions/" + subscription.getId()
        ));

        if (subscription.getEndsAt() != null) {
            params.put("endDate", DateUtils.formatInstantToString(subscription.getEndsAt()));
        }

        notificationService.save(NotificationReference.NEW_SUBSCRIPTION, subscription.getClient(), params, false);
    }

    private String buildRedirectUrl(String path) {
        return frontBaseUrl + "/client/" + path;
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
            invoice.voidInvoice();
            log.info("Invoice {} voided successfully.", invoice.getId());
        } catch (StripeException e) {
            log.error("Error when voiding the last invoice: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public String generateCustomerPortalSession(Client client) throws StripeException {
        if (!repository.existsByClientId(client.getId())) {
            throw new ForbiddenException(SubscriptionValidationMessages.CLIENT_WITHOUT_SUBSCRIPTIONS);
        }

        if (ObjectUtils.isEmpty(client.getStripeCustomerId())) {
            throw new ForbiddenException(SubscriptionValidationMessages.CLIENT_WITHOUT_STRIPE_ID);
        }

        try {
            Customer customer = clientHelper.getOrCreateCustomer(client);
            SessionCreateParams params = SessionCreateParams.builder()
                    .setCustomer(customer.getId())
                    .setReturnUrl(buildRedirectUrl("subscriptions"))
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Error when creating customer portal session for clientStripeCustomerId: {}, error: {}", client.getStripeCustomerId(), e.getMessage());
            throw e;
        }

    }
}
