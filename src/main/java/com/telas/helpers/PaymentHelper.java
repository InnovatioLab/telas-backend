package com.telas.helpers;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.InvoicePaymentListParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.telas.dtos.EmailDataDto;
import com.telas.entities.Client;
import com.telas.entities.Payment;
import com.telas.entities.Subscription;
import com.telas.enums.PaymentStatus;
import com.telas.enums.Recurrence;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.ClientRepository;
import com.telas.services.EmailService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.PaymentValidationMessages;
import com.telas.shared.utils.MoneyUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class PaymentHelper {
  private final Logger log = LoggerFactory.getLogger(PaymentHelper.class);
  private final SubscriptionHelper subscriptionHelper;
  private final ClientRepository clientRepository;
  private final EmailService emailService;

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
            .setDescription("Invoice payment for your tela's subscription for monitors: " + subscription.getMonitorAddresses());

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
    return subscriptionHelper.findEntityById(subscriptionId);
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
  public void handleCompletedPayment(Subscription subscription, Payment payment, PaymentIntent paymentIntent) {
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

    handleSuccessfulPayment(payment, false);
  }

  @Transactional
  public void handleCompletedPaymentFromInvoice(Subscription subscription, Payment payment, Invoice invoice) {
    boolean isRecurringPayment = invoice.getBillingReason() != null && invoice.getBillingReason().equals("subscription_cycle");

    if (subscription.getStartedAt() == null) {
      updateSubscriptionPeriod(invoice, subscription);
    }

    if (ValidateDataUtils.isNullOrEmptyString(subscription.getStripeId()) && !ValidateDataUtils.isNullOrEmptyString(invoice.getParent().getSubscriptionDetails().getSubscription())) {
      subscription.setStripeId(invoice.getParent().getSubscriptionDetails().getSubscription());
    }


    String recurrenceStr = invoice.getParent().getSubscriptionDetails().getMetadata().get("recurrence");
    if (!ValidateDataUtils.isNullOrEmptyString(recurrenceStr) && subscription.isUpgrade()) {
      Recurrence recurrence = Recurrence.valueOf(recurrenceStr);
      if (Recurrence.MONTHLY.equals(recurrence)) {
        subscription.setRecurrence(recurrence);
        subscription.setEndsAt(null);
      }
    }

    handleSuccessfulPayment(payment, isRecurringPayment);
  }

  private void handleSuccessfulPayment(Payment payment, boolean isRecurringPayment) {
    log.info("Payment succeeded with id: {}", payment.getId());
    Subscription subscription = payment.getSubscription();

    if (subscription.isUpgrade()) {
      log.info("Skipping cart inactivation for upgraded subscription: {}", subscription.getId());
      subscription.setUpgrade(false);
      return;
    }

    if (!isRecurringPayment) {
      handleNonRecurringPayment(subscription);
    }
  }

  private void handleNonRecurringPayment(Subscription subscription) {
    Client client = subscription.getClient();
    subscriptionHelper.inactivateCart(client);
    subscriptionHelper.deleteSubscriptionFlow(client);
    clientRepository.save(client);

    if (!subscription.isUpgrade()) {
      sendFirstBuyEmail(subscription);
    }
  }

  @Transactional
  public void updateAuditInfo(Subscription subscription) {
    CustomRevisionListener.setUsername("Stripe Webhook");
    subscription.setUsernameUpdate("Stripe Webhook");
    subscription.getPayments().forEach(payment -> payment.setUsernameUpdate("Stripe Webhook"));
  }

  @Transactional
  public com.stripe.model.Subscription getStripeSubscription(Subscription subscription) throws StripeException {
    return subscriptionHelper.getStripeSubscription(subscription);
  }

  private void sendFirstBuyEmail(Subscription subscription) {
    Map<String, String> params = new HashMap<>(Map.of(
            "name", subscription.getClient().getBusinessName(),
            "locations", String.join(", ", subscription.getMonitorAddresses()),
            "startDate", formatDate(subscription.getStartedAt())
    ));

    if (subscription.getEndsAt() != null) {
      params.put("endDate", formatDate(subscription.getEndsAt()));
    }

    String email = subscription.getClient().getContact().getEmail();
    EmailDataDto emailData = new EmailDataDto(
            email,
            SharedConstants.TEMPLATE_EMAIL_FIRST_SUBSCRIPTION,
            SharedConstants.EMAIL_SUBJECT_FIRST_SUBSCRIPTION,
            params
    );
    emailService.send(emailData);
  }

  private String formatDate(Instant date) {
    return DateTimeFormatter.ofPattern("MM/dd/yyyy")
            .withZone(ZoneId.of(SharedConstants.ZONE_ID))
            .format(date);
  }
}
