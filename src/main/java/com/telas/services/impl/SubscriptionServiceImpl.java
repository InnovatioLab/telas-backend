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
import com.telas.scheduler.SchedulerJobRunContext;
import com.telas.services.RemoveMonitorAdsOutcome;
import com.telas.services.SubscriptionService;
import com.telas.shared.constants.valitation.SubscriptionValidationMessages;
import com.telas.shared.utils.PaginationFilterUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static java.util.Locale.US;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final int SCHEDULER_SUMMARY_MAX_ITEMS = 80;

    private final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);
    private final SubscriptionRepository repository;
    private final ClientRepository clientRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final SubscriptionHelper helper;
    private final SchedulerJobRunContext schedulerJobRunContext;

    @Override
    @Transactional
    public String save() {
        Client client = authenticatedUserService.getLoggedUser().client();

        if (client.isPrivilegedPanelUser()) {
            return null;
        }

        Cart cart = helper.getAndValidateActiveCart(client);
        Subscription subscription = new Subscription(client, cart);

        if (subscription.isBonus()) {
            return null;
        }

        persistSubscriptionClient(client, subscription);
        return helper.process(subscription, null);
    }

    @Override
    @Transactional
    public void savePartnerBonusSubscription(Client partner, Monitor monitor) {
        if (repository.findActiveBonusSubscriptionByClientId(partner.getId()).isPresent()) {
            log.info("Partner with id: {} already has a bonus subscription, skipping creation.", partner.getId());
            return;
        }

        if (Role.PARTNER.equals(partner.getRole()) && Objects.equals(monitor.getAddress().getClient().getId(), partner.getId())) {
            log.info("Partner with id: {} is eligible for a bonus subscription.", partner.getId());
            Subscription subscription = new Subscription(partner, monitor);
            log.info("Subscription generated with id: {} for partner id: {}", subscription.getId(), partner.getId());

            try {
                persistSubscriptionClient(partner, subscription);
                helper.handleBonusSubscription(subscription);
            } catch (DataIntegrityViolationException e) {
                log.warn("Could not create bonus subscription for a partner {} due to constraint violation. Likely concurrent creation.", partner.getId());
            }
        }
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
        return helper.process(entity, recurrence);
    }

    @Override
    public String generateCustomerPortalSession() throws StripeException {
        Client client = authenticatedUserService.getLoggedUser().client();

        if (client.isPrivilegedPanelUser()) {
            return null;
        }
        return helper.generateCustomerPortalSession(client);
    }

    @Override
    @Transactional
    public String renewSubscription(UUID subscriptionId) {
        Client client = authenticatedUserService.getLoggedUser().client();

        if (client.isPrivilegedPanelUser()) {
            return null;
        }

        Subscription entity = helper.findEntityById(subscriptionId);
        authenticatedUserService.validateSelfOrAdmin(entity.getClient().getId());
        helper.validateSubscriptionForRenewal(entity, client);
        return helper.process(entity, entity.getRecurrence());
    }

    @Override
    @Transactional
    public void cancelSubscription(UUID subscriptionId) {
        Subscription subscription = helper.findEntityById(subscriptionId);

        if (!SubscriptionStatus.ACTIVE.equals(subscription.getStatus())) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_CANCEL_NOT_ALLOWED_FOR_NON_ACTIVE);
        }

        if (subscription.isCancelAtPeriodEnd()) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_ALREADY_SCHEDULED_TO_CANCEL);
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
    public void cancelBonusSubscription(Client partner) {
        Subscription subscription = repository.findActiveBonusSubscriptionByClientId(partner.getId())
                .orElseThrow(() -> new ResourceNotFoundException(SubscriptionValidationMessages.BONUS_SUBSCRIPTION_NOT_FOUND + partner.getId()));
        updateSubscriptionStatusCancelled(subscription, Instant.now(), subscription.getClient().getBusinessName());
        helper.removeMonitorAdsFromSubscription(subscription);
        notifyClientsWishList(subscription);
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
    @Scheduled(
            cron = "${subscription.cron.remove-expired-ads:0 0 4 * * *}",
            zone = "${app.scheduler.zone:America/New_York}")
    @SchedulerLock(name = "removeAdsFromExpiredSubscriptionsLock", lockAtLeastFor = "PT10M", lockAtMostFor = "PT1H")
    public void removeAdsFromExpiredSubscriptions() {
        List<Subscription> expiredSubscriptions = repository.getActiveAndExpiredSubscriptions(Instant.now());

        if (expiredSubscriptions.isEmpty()) {
            log.info("No expired subscriptions found.");
            schedulerJobRunContext.putAll(buildRemoveExpiredAdsSummary(0, List.of(), false));
            return;
        }

        log.info("Found {} expired subscriptions, removing ads.", expiredSubscriptions.size());

        List<Map<String, Object>> items = new ArrayList<>();
        boolean truncated = false;
        for (Subscription subscription : expiredSubscriptions) {
            log.info("Removing ads from expired subscription with id: {}", subscription.getId());
            subscription.setUsernameUpdate("Virtual Assistant");
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            RemoveMonitorAdsOutcome outcome = helper.removeMonitorAdsFromSubscription(subscription);
            repository.save(subscription);
            notifyClientsWishList(subscription);
            if (items.size() < SCHEDULER_SUMMARY_MAX_ITEMS) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("subscriptionId", subscription.getId().toString());
                row.put("clientBusinessName", subscription.getClient().getBusinessName());
                row.put("removedAdNames", outcome.removedAdNames());
                items.add(row);
            } else {
                truncated = true;
            }
        }
        schedulerJobRunContext.putAll(buildRemoveExpiredAdsSummary(expiredSubscriptions.size(), items, truncated));
    }

    @Override
    @Transactional
    @Scheduled(
            cron = "${subscription.cron.expiry-emails:0 0 6 * * *}",
            zone = "${app.scheduler.zone:America/New_York}")
    @SchedulerLock(name = "sendSubscriptionExpirationEmailLock", lockAtLeastFor = "PT10M", lockAtMostFor = "PT30M")
    public void sendSubscriptionExpirationEmail() {
        LocalDate today = LocalDate.now();
        LocalDate in15Days = today.plusDays(15);
        LocalDate in10Days = today.plusDays(10);
        LocalDate in5Days = today.plusDays(5);
        LocalDate in3Days = today.plusDays(3);
        LocalDate endDateOnTomorrow = today.plusDays(1);

        List<Subscription> subscriptionsReminder15 = repository.findSubscriptionsWithEndsAtOnDate(java.sql.Date.valueOf(in15Days));
        List<Subscription> subscriptionsReminder10 = repository.findSubscriptionsWithEndsAtOnDate(java.sql.Date.valueOf(in10Days));
        List<Subscription> subscriptionsReminder5 = repository.findSubscriptionsWithEndsAtOnDate(java.sql.Date.valueOf(in5Days));
        List<Subscription> subscriptionsReminder3 = repository.findSubscriptionsWithEndsAtOnDate(java.sql.Date.valueOf(in3Days));
        List<Subscription> subscriptionsPenultimateDay = repository.findSubscriptionsWithEndsAtOnDate(java.sql.Date.valueOf(endDateOnTomorrow));

        if (!subscriptionsReminder15.isEmpty()) {
            log.info("Found {} subscriptions expiring in 15 days, sending reminder emails.", subscriptionsReminder15.size());
            subscriptionsReminder15.forEach(helper::sendSubscriptionAboutToExpiryEmail);
        }

        if (!subscriptionsReminder10.isEmpty()) {
            log.info("Found {} subscriptions expiring in 10 days, sending reminder emails.", subscriptionsReminder10.size());
            subscriptionsReminder10.forEach(helper::sendSubscriptionTenDaysBeforeExpiryEmail);
        }

        if (!subscriptionsReminder5.isEmpty()) {
            log.info("Found {} subscriptions expiring in 5 days, sending reminder emails.", subscriptionsReminder5.size());
            subscriptionsReminder5.forEach(helper::sendSubscriptionFiveDaysBeforeExpiryEmail);
        }

        if (!subscriptionsReminder3.isEmpty()) {
            log.info("Found {} subscriptions expiring in 3 days, sending reminder emails.", subscriptionsReminder3.size());
            subscriptionsReminder3.forEach(helper::sendSubscriptionThreeDaysBeforeExpiryEmail);
        }

        if (!subscriptionsPenultimateDay.isEmpty()) {
            log.info("Found {} subscriptions on penultimate day (end date tomorrow), sending final reminder emails.", subscriptionsPenultimateDay.size());
            subscriptionsPenultimateDay.forEach(helper::sendSubscriptionPenultimateDayEmail);
        }

        if (subscriptionsReminder15.isEmpty() && subscriptionsReminder10.isEmpty() && subscriptionsReminder5.isEmpty()
                && subscriptionsReminder3.isEmpty() && subscriptionsPenultimateDay.isEmpty()) {
            log.info("No subscription expiry reminders to send (15/10/5/3 days or penultimate day).");
        }

        Map<String, Object> emailSummary = new LinkedHashMap<>();
        emailSummary.put("reminder15", buildExpiryEmailBucket(subscriptionsReminder15));
        emailSummary.put("reminder10", buildExpiryEmailBucket(subscriptionsReminder10));
        emailSummary.put("reminder5", buildExpiryEmailBucket(subscriptionsReminder5));
        emailSummary.put("reminder3", buildExpiryEmailBucket(subscriptionsReminder3));
        emailSummary.put("penultimate", buildExpiryEmailBucket(subscriptionsPenultimateDay));
        schedulerJobRunContext.putAll(emailSummary);
    }

    private Map<String, Object> buildRemoveExpiredAdsSummary(
            int subscriptionsProcessed, List<Map<String, Object>> items, boolean truncated) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("subscriptionsProcessed", subscriptionsProcessed);
        m.put("items", items);
        if (truncated) {
            m.put("truncated", true);
        }
        return m;
    }

    private Map<String, Object> buildExpiryEmailBucket(List<Subscription> subscriptions) {
        Map<String, Object> bucket = new LinkedHashMap<>();
        bucket.put("count", subscriptions.size());
        List<Map<String, Object>> items = new ArrayList<>();
        boolean truncated = false;
        for (Subscription s : subscriptions) {
            if (items.size() >= SCHEDULER_SUMMARY_MAX_ITEMS) {
                truncated = true;
                break;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("subscriptionId", s.getId().toString());
            row.put("clientBusinessName", s.getClient().getBusinessName());
            row.put("clientEmail", s.getClient().getContact().getEmail());
            items.add(row);
        }
        bucket.put("items", items);
        if (truncated) {
            bucket.put("truncated", true);
        }
        return bucket;
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

            if (client.isPrivilegedPanelUser()) {
                log.info("Subscription with id: {} set to cancel NOW by admin, removing monitors ads", subscription.getId());
                stripeSubscription.cancel();
            } else {
                stripeSubscription.update(Map.of("cancel_at_period_end", true));
                log.info("Subscription with id: {} set to cancel at the end of the billing period.", subscription.getId());
                subscription.setCancelAtPeriodEnd(true);
                helper.setAuditInfo(subscription, client.getBusinessName());
                repository.save(subscription);
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

            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(
                            root.join("subscriptionMonitors").join("id").join("monitor").get("address").get("street")
                    ), filter));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("status")), filter));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("recurrence")), filter));

            addDatePredicates(predicates, criteriaBuilder, root, genericFilter);
            addIdPredicate(predicates, criteriaBuilder, root, genericFilter);
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        });
    }

    private void addDatePredicates(List<Predicate> predicates, CriteriaBuilder criteriaBuilder, Root<Subscription> root, String genericFilter) {
        try {
            LocalDate date = LocalDate.parse(genericFilter);
            predicates.add(criteriaBuilder.equal(criteriaBuilder.function("date", LocalDate.class, root.get("startedAt")), date));
            predicates.add(criteriaBuilder.equal(criteriaBuilder.function("date", LocalDate.class, root.get("endsAt")), date));
            return;
        } catch (DateTimeParseException ignored) {
        }

        try {
            DateTimeFormatter usFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", US);
            LocalDate date = LocalDate.parse(genericFilter, usFormatter);
            predicates.add(criteriaBuilder.equal(criteriaBuilder.function("date", LocalDate.class, root.get("startedAt")), date));
            predicates.add(criteriaBuilder.equal(criteriaBuilder.function("date", LocalDate.class, root.get("endsAt")), date));
        } catch (DateTimeParseException ignored) {
        }
    }


    private void addIdPredicate(List<Predicate> predicates, CriteriaBuilder criteriaBuilder, Root<Subscription> root, String genericFilter) {
        try {
            UUID id = UUID.fromString(genericFilter);
            predicates.add(criteriaBuilder.equal(root.get("id"), id));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void persistSubscriptionClient(Client client, Subscription subscription) {
        repository.save(subscription);
        client.getSubscriptions().add(subscription);
        clientRepository.save(client);
    }
}
