package com.telas.services.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.telas.dtos.request.filters.SubscriptionFilterRequestDto;
import com.telas.dtos.response.PaginationResponseDto;
import com.telas.dtos.response.SubscriptionMinResponseDto;
import com.telas.dtos.response.SubscriptionResponseDto;
import com.telas.entities.Cart;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.Subscription;
import com.telas.enums.Recurrence;
import com.telas.enums.Role;
import com.telas.enums.SubscriptionStatus;
import com.telas.helpers.SubscriptionHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.PaymentService;
import com.telas.services.SubscriptionService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.SubscriptionValidationMessages;
import com.telas.shared.utils.PaginationFilterUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
    private final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);
    private final SubscriptionRepository repository;
    private final ClientRepository clientRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final PaymentService paymentService;
    private final SubscriptionHelper helper;

    @Override
    @Transactional
    public String save() {
        Client client = authenticatedUserService.getLoggedUser().client();

        if (Role.ADMIN.equals(client.getRole())) {
            return null;
        }

        Cart cart = helper.getAndValidateActiveCart(client);
        Subscription subscription = new Subscription(client, cart);

        if (subscription.isBonus()) {
            log.info("Client with id: {} is eligible for a bonus subscription", client.getId());
            persistSubscriptionClient(client, subscription);
            helper.handleBonusSubscriptionOrNonRecurringPayment(subscription);
            return helper.getRedirectUrlAfterCreatingNewSubscription(client);
        }

        persistSubscriptionClient(client, subscription);
        return paymentService.process(subscription, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponseDto findById(UUID subscriptionId) {
        Subscription entity = helper.findEntityById(subscriptionId);
        Client loggedUser = authenticatedUserService.validateSelfOrAdmin(entity.getClient().getId()).client();
        return helper.getSubscriptionResponse(entity, loggedUser);
    }

    @Override
    @Transactional
    public String upgradeSubscription(UUID subscriptionId, Recurrence recurrence) {
        Subscription entity = helper.findEntityById(subscriptionId);
        authenticatedUserService.validateSelfOrAdmin(entity.getClient().getId());
        helper.validateSubscriptionForUpgrade(entity, recurrence);
        entity.setUpgrade(true);
        repository.save(entity);
        return paymentService.process(entity, recurrence);

    }

    @Override
    @Transactional
    public void cancelSubscription(UUID subscriptionId) {
        Subscription subscription = helper.findEntityById(subscriptionId);

        if (!SubscriptionStatus.ACTIVE.equals(subscription.getStatus())) {
            log.info("Subscription with id: {} isn't active", subscriptionId);
            return;
        }

        Client client = authenticatedUserService.validateSelfOrAdmin(subscription.getClient().getId()).client();

        if (Recurrence.MONTHLY.equals(subscription.getRecurrence()) && !subscription.isBonus()) {
            handleStripeCancellation(subscription, client);
        } else {
            updateSubscriptionStatusCancelled(subscription, Instant.now(), client.getBusinessName());
            helper.removeMonitorAdsFromSubscription(subscription);
            notifyClientsWishList(subscription);
        }
    }

    @Override
    @Transactional
    public void cancelSubscription(com.stripe.model.Subscription stripeSubscription) {
        UUID subscriptionId = UUID.fromString(stripeSubscription.getMetadata().get("subscriptionId"));
        Subscription subscription = helper.findEntityById(subscriptionId);

        if (isInvalidStatus(subscription.getStatus())) {
            log.info("Subscription with id: {} is already cancelled or expired.", subscriptionId);
            return;
        }

        Instant endedAt = Instant.ofEpochSecond(stripeSubscription.getEndedAt());
        updateSubscriptionStatusCancelled(subscription, endedAt, "Stripe Webhook");
        helper.removeMonitorAdsFromSubscription(subscription);
        notifyClientsWishList(subscription);
        helper.voidLatestInvoice(stripeSubscription);
    }

    @Override
    @Transactional
    public void handleCheckoutSessionExpired(Session session) {
        String subscriptionId = session.getClientReferenceId();
        log.info("Handling checkout session expired for subscription id: {}", subscriptionId);

        try {
            Subscription subscription = helper.findEntityById(UUID.fromString(subscriptionId));

            if (SubscriptionStatus.ACTIVE.equals(subscription.getStatus()) && subscription.isUpgrade()) {
                log.info("Subscription with id: {} is active and on upgrade, setting upgrade to false.", subscriptionId);
                subscription.setUpgrade(false);
                repository.save(subscription);
            }
        } catch (ResourceNotFoundException e) {
            log.warn("Subscription with id: {}, not found for checkout session expired.", subscriptionId);
        }
    }

    @Override
    @Transactional
    @Scheduled(cron = SharedConstants.DAILY_CRON, zone = SharedConstants.ZONE_ID)
    @SchedulerLock(name = "removeAdsFromExpiredSubscriptionsLock", lockAtLeastFor = "PT10M", lockAtMostFor = "PT1H")
    public void removeAdsFromExpiredSubscriptions() {
        List<Subscription> expiredSubscriptions = repository.getActiveAndExpiredSubscriptions(Instant.now());

        if (expiredSubscriptions.isEmpty()) {
            log.info("No expired subscriptions found.");
            return;
        }

        log.info("Found {} expired subscriptions, removing ads.", expiredSubscriptions.size());

        expiredSubscriptions.forEach(subscription -> {
            log.info("Removing ads from expired subscription with id: {}", subscription.getId());
            subscription.setUsernameUpdate("Virtual Assistant");
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            helper.removeMonitorAdsFromSubscription(subscription);
            repository.save(subscription);
            notifyClientsWishList(subscription);
        });
    }

    @Override
    @Transactional
    @Scheduled(cron = SharedConstants.EXPIRY_SUBSCRIPTION_CRON, zone = SharedConstants.ZONE_ID)
    @SchedulerLock(name = "sendSubscriptionExpirationEmailLock", lockAtLeastFor = "PT10M", lockAtMostFor = "PT30M")
    public void sendSubscriptionExpirationEmail() {
        Instant fifteenDays = Instant.now().plus(15, ChronoUnit.DAYS);
        Instant now = Instant.now();
        List<Subscription> subscriptionsReminder = repository.findSubscriptionsExpiringExactlyOn(fifteenDays);
        List<Subscription> subscriptionsExpiringToday = repository.findSubscriptionsExpiringExactlyOn(now);

        if (!subscriptionsReminder.isEmpty()) {
            log.info("Found {} subscriptions expiring in 15 days, sending reminder emails.", subscriptionsReminder.size());
            subscriptionsReminder.forEach(helper::sendSubscriptionAboutToExpiryEmail);
        }

        if (!subscriptionsExpiringToday.isEmpty()) {
            log.info("Found {} subscriptions expiring today, sending last day emails.", subscriptionsExpiringToday.size());
            subscriptionsExpiringToday.forEach(helper::sendSubscriptionExpiryTodayEmail);
        }

        if (subscriptionsReminder.isEmpty() && subscriptionsExpiringToday.isEmpty()) {
            log.info("No subscriptions expiring in 15 days or today.");
        }
    }


    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<SubscriptionMinResponseDto>> findClientSubscriptionsFilters(SubscriptionFilterRequestDto request) {
        Client client = authenticatedUserService.getLoggedUser().client();
        Sort order = request.setOrdering();

        Pageable pageable = PaginationFilterUtil.getPageable(request, order);
        Specification<Subscription> filter = PaginationFilterUtil.addSpecificationFilter(
                (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("client").get("id"), client.getId()),
                request.getGenericFilter(),
                this::filterSubscriptions
        );

        Page<Subscription> page = repository.findAll(filter, pageable);
        List<SubscriptionMinResponseDto> response = page.stream().map(SubscriptionMinResponseDto::new).toList();
        return PaginationResponseDto.fromResult(response, (int) page.getTotalElements(), page.getTotalPages(), request.getPage());
    }

    private void notifyClientsWishList(Subscription subscription) {
        List<Client> clients = clientRepository.findAllByMonitorsInWishlist(subscription.getMonitors());
        Set<Monitor> monitors = subscription.getMonitors();

        if (clients.isEmpty() || monitors.isEmpty()) {
            log.info("No clients or monitors to notify.");
            return;
        }

        helper.notifyClientsWishList(clients, subscription.getMonitors());
    }

    private boolean isInvalidStatus(SubscriptionStatus status) {
        return List.of(SubscriptionStatus.CANCELLED, SubscriptionStatus.EXPIRED).contains(status);
    }

    private void updateSubscriptionStatusCancelled(Subscription subscription, Instant endsAt, String updatedBy) {
        log.info("Handling subscription deletion for id: {}", subscription.getId().toString());
        helper.setAuditInfo(subscription, updatedBy);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setEndsAt(endsAt);
        repository.save(subscription);
    }

    private void handleStripeCancellation(Subscription subscription, Client client) {
        try {
            com.stripe.model.Subscription stripeSubscription = helper.getStripeSubscription(subscription);

            if (Role.ADMIN.equals(client.getRole())) {
                log.info("Subscription with id: {} set to cancel NOW by admin, removing monitors ads", subscription.getId());
                stripeSubscription.cancel();
            } else {
                stripeSubscription.update(Map.of("cancel_at_period_end", true));
                log.info("Subscription with id: {} set to cancel at the end of the billing period.", subscription.getId());
            }

        } catch (StripeException e) {
            log.error("Error setting subscription with id: {} to cancel at period end", subscription.getId(), e);
            throw new BusinessRuleException(SubscriptionValidationMessages.RETRIEVE_STRIPE_SUBSCRIPTION_ERROR + subscription.getId());
        }
    }

    Specification<Subscription> filterSubscriptions(Specification<Subscription> specification, String genericFilter) {
        return specification.and((root, query, criteriaBuilder) -> {
            String filter = "%" + genericFilter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("monitors").get("address").get("street")), filter));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("status")), filter));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("recurrence")), filter));

            addDatePredicates(predicates, criteriaBuilder, root, genericFilter);
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        });
    }

    private void addDatePredicates(List<Predicate> predicates, CriteriaBuilder criteriaBuilder, Root<Subscription> root, String genericFilter) {
        try {
            LocalDate date = LocalDate.parse(genericFilter);
            predicates.add(criteriaBuilder.equal(criteriaBuilder.function("date", LocalDate.class, root.get("startedAt")), date));
            predicates.add(criteriaBuilder.equal(criteriaBuilder.function("date", LocalDate.class, root.get("endsAt")), date));
        } catch (DateTimeParseException ignored) {
        }
    }

    private void persistSubscriptionClient(Client client, Subscription subscription) {
        repository.save(subscription);
        client.getSubscriptions().add(subscription);
        clientRepository.save(client);
    }
}
