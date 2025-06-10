package com.telas.services.impl;

import com.stripe.exception.StripeException;
import com.telas.dtos.request.filters.SubscriptionFilterRequestDto;
import com.telas.dtos.response.PaginationResponseDto;
import com.telas.dtos.response.SubscriptionMinResponseDto;
import com.telas.dtos.response.SubscriptionResponseDto;
import com.telas.entities.Cart;
import com.telas.entities.Client;
import com.telas.entities.Subscription;
import com.telas.enums.Recurrence;
import com.telas.enums.SubscriptionStatus;
import com.telas.helpers.SubscriptionHelper;
import com.telas.infra.exceptions.BusinessRuleException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    Cart cart = helper.getActiveCart(client);
    Subscription subscription = new Subscription(client, cart);

    if (subscription.isBonus()) {
      subscription.initialize();
      subscription.setStatus(SubscriptionStatus.ACTIVE);
      persistSubscriptionClient(client, subscription);
      return null;
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
    helper.validateSubscriptionForUpgrade(entity);
    // Fazer alguma verificação se o upgrade dela já tava como true??
    entity.setUpgrade(true);
    repository.save(entity);


    return paymentService.process(entity, recurrence);

  }

  @Override
  @Transactional(readOnly = true)
  public boolean checkIfCanBeUpgraded(UUID subscriptionId, Recurrence recurrence) {
    Subscription entity = helper.findEntityById(subscriptionId);
    authenticatedUserService.validateSelfOrAdmin(entity.getClient().getId());
    return entity.canBeUpgraded(recurrence);
  }

  @Override
  @Transactional
  public void cancelSubscription(com.stripe.model.Subscription stripeSubscription) {
    UUID subscriptionId = UUID.fromString(stripeSubscription.getMetadata().get("subscriptionId"));
    Subscription subscription = helper.findEntityById(subscriptionId);
    List<SubscriptionStatus> invalidStatuses = List.of(SubscriptionStatus.CANCELLED, SubscriptionStatus.EXPIRED);

    if (invalidStatuses.contains(subscription.getStatus())) {
      log.info("Subscription with id: {} is already cancelled or expired.", subscriptionId);
      return;
    }

    log.info("Handling subscription deletion for id: {}", subscriptionId);
    helper.setAuditInfo(subscription, "Stripe Webhook");
    subscription.setStatus(SubscriptionStatus.CANCELLED);

    subscription.setEndsAt(Instant.ofEpochSecond(stripeSubscription.getEndedAt()));
    repository.save(subscription);
  }

  @Override
  @Transactional
  public void cancelSubscription(UUID subscriptionId) {
    Subscription subscription = helper.findEntityById(subscriptionId);

    if (!SubscriptionStatus.ACTIVE.equals(subscription.getStatus())) {
      log.info("Subscription with id: {} isn't active", subscriptionId);
      return;
    }

    if (!Recurrence.MONTHLY.equals(subscription.getRecurrence())) {
      log.info("Subscription with id: {} is not a monthly subscription, skipping cancellation.", subscriptionId);
      return;
    }

    authenticatedUserService.validateSelfOrAdmin(subscription.getClient().getId());

    try {
      com.stripe.model.Subscription stripeSubscription = helper.getStripeSubscription(subscription);
      stripeSubscription.update(Map.of("cancel_at_period_end", true));
      log.info("Subscription with id: {} set to cancel at the end of the billing period.", subscriptionId);
    } catch (StripeException e) {
      log.error("Error setting subscription with id: {} to cancel at period end", subscriptionId, e);
      throw new BusinessRuleException(SubscriptionValidationMessages.RETRIEVE_STRIPE_SUBSCRIPTION_ERROR + subscriptionId);
    }
  }

  @Override
  @Transactional
  @Scheduled(cron = SharedConstants.DAILY_CRON, zone = SharedConstants.ZONE_ID)
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
    });
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
