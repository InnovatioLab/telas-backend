package com.telas.helpers;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.InvoicePaymentListParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.dtos.response.MonitorValidationResponseDto;
import com.telas.dtos.response.SubscriptionMonitorResponseDto;
import com.telas.dtos.response.SubscriptionResponseDto;
import com.telas.entities.*;
import com.telas.entities.Subscription;
import com.telas.enums.PaymentStatus;
import com.telas.enums.Recurrence;
import com.telas.enums.SubscriptionStatus;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.SubscriptionFlowRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.BucketService;
import com.telas.services.CartService;
import com.telas.services.MonitorService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.CartValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.constants.valitation.PaymentValidationMessages;
import com.telas.shared.constants.valitation.SubscriptionValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.MoneyUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

  @Transactional
  public Cart getActiveCart(Client client) {
    Cart cart = cartService.findActiveByClientIdWithItens(client.getId());

    validateCart(cart);
    validateItems(cart.getItems());
    return cart;
  }

  @Transactional
  public void inactivateCart(Client client) {
    Cart cart = cartService.findActiveByClientIdWithItens(client.getId());
    cartService.inactivateCart(cart);
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
    List<UUID> monitorIds = items.stream()
            .map(item -> item.getMonitor().getId())
            .toList();

    Client client = items.get(0).getCart().getClient();

    List<MonitorValidationResponseDto> results = monitorService.findInvalidMonitorsDuringCheckout(monitorIds, client.getId());

    boolean hasInvalidMonitor = results.stream().anyMatch(result -> !result.isValidMonitor());

    if (hasInvalidMonitor) {
      throw new BusinessRuleException(MonitorValidationMessages.MONITOR_INACTIVE_OR_BLOCKS_UNAVAILABLE);
    }

    boolean hasActiveSubscriptionWithMonitor = results.stream().anyMatch(MonitorValidationResponseDto::isHasActiveSubscription);

    if (hasActiveSubscriptionWithMonitor) {
      throw new BusinessRuleException(SubscriptionValidationMessages.CLIENT_ALREADY_HAS_ACTIVE_SUBSCRIPTION_WITH_MONITOR);
    }
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
  public void updateSubscriptionPeriod(Invoice invoice, Subscription subscription) {
    List<Long> periods = invoice.getLines().getData().stream()
            .map(line -> line.getPeriod().getStart())
            .toList();

    if (!periods.isEmpty()) {
      subscription.setStartedAt(Instant.ofEpochSecond(Collections.min(periods)));
    }
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

  @Transactional(readOnly = true)
  public void validateSubscriptionForUpgrade(Subscription entity) {
    if (entity.isBonus()) {
      throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_BONUS);
    }

    if (Recurrence.MONTHLY.equals(entity.getRecurrence())) {
      throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_MONTHLY);
    }

    boolean isExpired = entity.getEndsAt() != null && !entity.getEndsAt().isAfter(Instant.now());
    boolean isInactive = !SubscriptionStatus.ACTIVE.equals(entity.getStatus());

    if (isInactive || isExpired) {
      throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_NON_ACTIVE_OR_EXPIRED);
    }

    if (!Recurrence.MONTHLY.equals(entity.getRecurrence()) && entity.getStartedAt() != null && entity.getEndsAt() != null) {
      long remainingTime = entity.getEndsAt().getEpochSecond() - Instant.now().getEpochSecond();

      if (remainingTime > SharedConstants.MAX_BILLING_CYCLE_ANCHOR) {
        throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_SHORT_BILLING_CYCLE);
      }
    }
  }

  @Transactional
  public Map<String, String> createMetaData(Subscription subscription, Payment payment, Recurrence recurrence) {
    Map<String, String> metaData = new HashMap<>();
    metaData.put("subscriptionId", subscription.getId().toString());
    metaData.put("clientId", subscription.getClient().getId().toString());
    metaData.put("paymentId", payment.getId().toString());

    if (recurrence != null) {
      metaData.put("recurrence", recurrence.name());
    }
    return metaData;
  }

  @Transactional
  public void configureSubscriptionParams(SessionCreateParams.Builder paramsBuilder, Subscription subscription, Map<String, String> metaData) throws StripeException {
    SessionCreateParams.SubscriptionData.Builder subscriptionDataBuilder = SessionCreateParams.SubscriptionData.builder()
            .putAllMetadata(metaData)
            .setDescription("Invoice payment for your tela's subscription");

    if (subscription.isUpgrade() && subscription.getEndsAt() != null) {
      addDiscountForUpgrade(paramsBuilder, subscription);
    }

    paramsBuilder.setSubscriptionData(subscriptionDataBuilder.build())
            .addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity((long) subscription.getMonitors().size())
                    .setPrice(getProductPriceIdMonthly())
                    .build());
  }

  @Transactional
  public void configurePaymentParams(SessionCreateParams.Builder paramsBuilder, Subscription subscription, Customer customer, Map<String, String> metaData) {
    BigDecimal totalPrice = calculatePrice(subscription.getMonitors().size());

    paramsBuilder.setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
            .setSetupFutureUsage(SessionCreateParams.PaymentIntentData.SetupFutureUsage.OFF_SESSION)
            .setReceiptEmail(customer.getEmail())
            .putAllMetadata(metaData)
            .build());

    paramsBuilder.addLineItem(
            SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            .setUnitAmount(totalPrice.multiply(BigDecimal.valueOf(100)).longValue())
                            .setProduct("prod_SP0KFP0uCSQrxt")
                            .build())
                    .build()
    );
  }

  private void addDiscountForUpgrade(SessionCreateParams.Builder paramsBuilder, Subscription subscription) throws StripeException {
    long now = Instant.now().getEpochSecond();
    long endsAt = subscription.getEndsAt().getEpochSecond();
    long remainingSeconds = endsAt - now;

    if (remainingSeconds > 0) {
      long totalSecondsInMonth = SharedConstants.MAX_BILLING_CYCLE_ANCHOR;
      double proportion = (double) remainingSeconds / totalSecondsInMonth;

      BigDecimal totalPrice = calculatePrice(subscription.getMonitors().size());
      BigDecimal adjustedPrice = MoneyUtils.multiply(totalPrice, BigDecimal.valueOf(proportion));

      CouponCreateParams couponParams = CouponCreateParams.builder()
              .setAmountOff(adjustedPrice.multiply(BigDecimal.valueOf(100)).longValue())
              .setCurrency("usd")
              .setDuration(CouponCreateParams.Duration.ONCE)
              .build();

      Coupon coupon = Coupon.create(couponParams);

      paramsBuilder.addDiscount(SessionCreateParams.Discount.builder()
              .setCoupon(coupon.getId())
              .build());
    }
  }

  private BigDecimal calculatePrice(int quantity) {
    BigDecimal totalPrice = BigDecimal.ZERO;

    for (int i = 1; i <= quantity; i++) {
      BigDecimal unitPrice = (i == 1) ? BigDecimal.valueOf(700)
              : (i == 2) ? BigDecimal.valueOf(600)
              : BigDecimal.valueOf(500);
      totalPrice = MoneyUtils.add(totalPrice, unitPrice);
    }

    return totalPrice;
  }

  private String getProductPriceIdMonthly() {
    try {
      PriceListParams params = PriceListParams.builder()
              .setProduct("prod_SP0KFP0uCSQrxt")
              .addAllLookupKey(List.of("subscription"))
              .build();

      List<Price> prices = Price.list(params).getData();

      return prices.stream()
              .filter(price -> price.getRecurring() != null && "month".equals(price.getRecurring().getInterval()))
              .map(Price::getId)
              .findFirst()
              .orElseThrow(() -> new ResourceNotFoundException(PaymentValidationMessages.PAYMENT_PRODUCT_PRICES_NOT_FOUND));
    } catch (StripeException e) {
      throw new BusinessRuleException(PaymentValidationMessages.PAYMENT_PRODUCT_PRICES_NOT_FOUND);
    }
  }

  @Transactional
  public void setPaymentMethodForInvoice(Invoice invoice, Payment payment) {
    try {
      InvoicePayment invoicePayment = InvoicePayment.list(
              InvoicePaymentListParams.builder()
                      .setLimit(1L)
                      .setInvoice(invoice.getId())
                      .build()
      ).getData().stream().findFirst().orElse(null);

      if (invoicePayment == null) {
        return;
      }

      PaymentIntent paymentIntent = PaymentIntent.retrieve(invoicePayment.getPayment().getPaymentIntent());
      setPaymentMethod(paymentIntent, payment);
    } catch (StripeException e) {
      log.error("Failed to set payment method for Invoice ID: {}, error: {}", invoice.getId(), e.getMessage());
    }
  }

  @Transactional
  public void setPaymentMethod(PaymentIntent paymentIntent, Payment payment) {
    try {
      PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentIntent.getPaymentMethod());
      payment.setPaymentMethod(paymentMethod == null ? "unknown" : paymentMethod.getType().toLowerCase());
    } catch (StripeException e) {
      log.error("Failed to set payment method for PaymentIntent ID: {}, error: {}", paymentIntent.getId(), e.getMessage());
    }
  }

  @Transactional
  public boolean isRecurringPayment(Subscription subscription, PaymentIntent paymentIntent) {
    String recurrenceStr = paymentIntent.getMetadata().get("recurrence");
    Recurrence recurrence = !ValidateDataUtils.isNullOrEmptyString(recurrenceStr) && subscription.isUpgrade()
            ? Recurrence.valueOf(recurrenceStr)
            : null;

    return Recurrence.MONTHLY.equals(subscription.getRecurrence()) || Recurrence.MONTHLY.equals(recurrence);
  }

  @Transactional
  public Subscription getSubscriptionFromInvoice(Invoice invoice) {
    UUID subscriptionId = UUID.fromString(invoice.getParent().getSubscriptionDetails().getMetadata().get("subscriptionId"));
    return repository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException(SubscriptionValidationMessages.SUBSCRIPTION_NOT_FOUND));
  }

  @Transactional
  public Payment getOrCreatePayment(Invoice invoice, Subscription subscription) {
    boolean isRecurringPayment = "subscription_cycle".equals(invoice.getBillingReason());

    return (!isRecurringPayment || subscription.isUpgrade())
            ? subscription.getPayments().stream()
            .filter(p -> PaymentStatus.PENDING.equals(p.getStatus()) && p.getStripeId() == null)
            .findFirst()
            .orElseGet(() -> new Payment(subscription))
            : new Payment(subscription);
  }

  @Transactional
  public void updatePaymentDetails(Payment payment, PaymentIntent paymentIntent) {
    payment.setStatus(PaymentStatus.fromStripeStatus(paymentIntent.getStatus(), null, payment));
    payment.setStripeId(paymentIntent.getId());
    setPaymentMethod(paymentIntent, payment);

    BigDecimal amountCharged = paymentIntent.getAmount() != null
            ? MoneyUtils.divide(BigDecimal.valueOf(paymentIntent.getAmount()), BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

    payment.setAmount(amountCharged);
  }

  @Transactional
  public void updatePaymentDetailsFromInvoice(Payment payment, Invoice invoice) {
    payment.setStatus(PaymentStatus.fromStripeStatus(null, invoice.getStatus(), payment));
    payment.setStripeId(invoice.getId());
    setPaymentMethodForInvoice(invoice, payment);

    BigDecimal amountDue = invoice.getAmountDue() != null
            ? MoneyUtils.divide(BigDecimal.valueOf(invoice.getAmountDue()), BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

    payment.setAmount(amountDue);
  }

  @Transactional
  public void handleCompletedPayment(Subscription subscription, PaymentIntent paymentIntent) {
    String recurrenceStr = paymentIntent.getMetadata().get("recurrence");
    Recurrence recurrence = !ValidateDataUtils.isNullOrEmptyString(recurrenceStr) && subscription.isUpgrade()
            ? Recurrence.valueOf(recurrenceStr)
            : null;

    if (recurrence != null) {
      subscription.setRecurrence(recurrence);
      subscription.setEndsAt(recurrence.calculateEndsAt(subscription.getEndsAt()));
    } else {
      subscription.initialize();
    }
  }

  @Transactional
  public void handleCompletedPaymentFromInvoice(Subscription subscription, Invoice invoice) {
    if (subscription.getStartedAt() == null) {
      updateSubscriptionPeriod(invoice, subscription);
    }

    String recurrenceStr = invoice.getParent().getSubscriptionDetails().getMetadata().get("recurrence");
    if (!ValidateDataUtils.isNullOrEmptyString(recurrenceStr) && subscription.isUpgrade()) {
      Recurrence recurrence = Recurrence.valueOf(recurrenceStr);
      if (Recurrence.MONTHLY.equals(recurrence)) {
        subscription.setRecurrence(recurrence);
        subscription.setEndsAt(null);
      }
    }
  }

  @Transactional
  public void updateAuditInfo(Subscription subscription) {
    CustomRevisionListener.setUsername("Stripe Webhook");
    subscription.setUsernameUpdate("Stripe Webhook");
    subscription.getPayments().forEach(payment -> payment.setUsernameUpdate("Stripe Webhook"));
  }
}
