package com.telas.services.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import com.telas.entities.Address;
import com.telas.entities.Client;
import com.telas.entities.Payment;
import com.telas.entities.Subscription;
import com.telas.enums.PaymentStatus;
import com.telas.enums.Recurrence;
import com.telas.enums.SubscriptionStatus;
import com.telas.helpers.SubscriptionHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.PaymentRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.PaymentService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.constants.valitation.PaymentValidationMessages;
import com.telas.shared.constants.valitation.SubscriptionValidationMessages;
import com.telas.shared.utils.MoneyUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
  private final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
  private final PaymentRepository repository;
  private final SubscriptionRepository subscriptionRepository;
  private final ClientRepository clientRepository;
  private final SubscriptionHelper subscriptionHelper;

  @Override
  @Transactional
  public String process(Subscription subscription, Recurrence recurrence) {
    try {
//      Stripe.apiKey = key;
      Payment payment = new Payment(subscription);
      subscription.getPayments().add(payment);
      repository.save(payment);
      return generateSession(subscription, payment, recurrence);
    } catch (StripeException e) {
      log.error("Error during processing payment for subscription with id: {}, error message: {}", subscription.getId(), e.getMessage());
      throw new BusinessRuleException(PaymentValidationMessages.PAYMENT_PROCESSING_ERROR);
    }
  }

  @Override
  @Transactional
  public void updatePaymentStatus(PaymentIntent paymentIntent) {
    try {
      String paymentIdString = paymentIntent.getMetadata().get("paymentId");

      if (ValidateDataUtils.isNullOrEmptyString(paymentIdString)) {
        log.error("Payment ID is missing in the metadata of the payment intent with id: {}", paymentIntent.getId());
        return;
      }

      UUID paymentId = UUID.fromString(paymentIdString);
      Payment payment = findEntityById(paymentId);

      String recurrenceStr = paymentIntent.getMetadata().get("recurrence");

      Recurrence recurrence;

      if (!ValidateDataUtils.isNullOrEmptyString(recurrenceStr) && payment.getSubscription().isUpgrade()) {
        recurrence = Recurrence.valueOf(recurrenceStr);
      } else {
        recurrence = null;
      }

      if (Recurrence.MONTHLY.equals(payment.getSubscription().getRecurrence()) || Recurrence.MONTHLY.equals(recurrence)) {
        return; // Não processa pagamentos recorrentes aqui
      }

      verifyClientOwnership(paymentIntent.getCustomer(), payment);

      Subscription subscription = payment.getSubscription();
      setAuditInfo(subscription);

      if (!subscription.isUpgrade()) {
        SubscriptionStatus subscriptionStatus = SubscriptionStatus.fromStripeStatus(paymentIntent.getStatus(), null, subscription);
        subscription.setStatus(subscriptionStatus);
      }

      PaymentStatus paymentStatus = PaymentStatus.fromStripeStatus(paymentIntent.getStatus(), null, payment);
      payment.setStatus(paymentStatus);
      payment.setStripeId(paymentIntent.getId());
      setPaymentMethod(paymentIntent, payment);

      BigDecimal amountCharged = paymentIntent.getAmount() != null
              ? MoneyUtils.divide(BigDecimal.valueOf(paymentIntent.getAmount()), BigDecimal.valueOf(100))
              : BigDecimal.ZERO;

      payment.setAmount(amountCharged);

      if (PaymentStatus.COMPLETED.equals(paymentStatus)) {
        if (recurrence != null) {
          subscription.setRecurrence(recurrence);
          Instant newEndsAt = recurrence.calculateEndsAt(subscription.getEndsAt());
          subscription.setEndsAt(newEndsAt);
        } else {
          subscription.initialize();
        }

        handleSuccessfulPayment(payment, false);
//        updateCustomerDefaultSource(paymentIntent.getCustomer(), payment.getPaymentMethod());
      } else if (PaymentStatus.FAILED.equals(paymentStatus)) {
        handleFailedPayment(payment);
      }

      finalizePayment(payment);
    } catch (StripeException e) {
      log.error("Error processing payment intent with id: {}, error message: {}", paymentIntent.getId(), e.getMessage());
      throw new BusinessRuleException(PaymentValidationMessages.PAYMENT_INTENT_PROCESSING_ERROR + paymentIntent.getId());
    }
  }

  @Override
  @Transactional
  public void updatePaymentStatus(Invoice invoice) {
    boolean isRecurringPayment = invoice.getBillingReason() != null && invoice.getBillingReason().equals("subscription_cycle");
    UUID subscriptionId = UUID.fromString(invoice.getParent().getSubscriptionDetails().getMetadata().get("subscriptionId"));
    Subscription subscription = subscriptionHelper.findEntityById(subscriptionId);

    setAuditInfo(subscription);

    Payment payment;
    if (!isRecurringPayment || subscription.isUpgrade()) {
      payment = subscription.getPayments().stream()
              .filter(p -> PaymentStatus.PENDING.equals(p.getStatus()) && p.getStripeId() == null)
              .findFirst()
              .orElseGet(() -> new Payment(subscription));
    } else {
      payment = new Payment(subscription);
    }

    subscription.getPayments().add(payment);
    payment.setStripeId(invoice.getId());
    verifyClientOwnership(invoice.getCustomer(), payment);

    PaymentStatus paymentStatus = PaymentStatus.fromStripeStatus(null, invoice.getStatus(), payment);
    payment.setStatus(paymentStatus);

    if (!subscription.isUpgrade()) {
      SubscriptionStatus subscriptionStatus = SubscriptionStatus.fromStripeStatus(null, invoice.getStatus(), subscription);
      subscription.setStatus(subscriptionStatus);
    }

    BigDecimal amountDue = invoice.getAmountDue() != null
            ? MoneyUtils.divide(BigDecimal.valueOf(invoice.getAmountDue()), BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
    payment.setAmount(amountDue);

    if (PaymentStatus.COMPLETED.equals(paymentStatus)) {
      if (subscription.getStartedAt() == null) {
        subscriptionHelper.updateSubscriptionPeriod(invoice, subscription);
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
    } else if (PaymentStatus.FAILED.equals(paymentStatus)) {
      handleFailedPayment(payment);
    }

    finalizePayment(payment);
  }

  @Override
  @Transactional
  public String createStripeSubscription(Subscription subscription) {
    try {
      Payment payment = savePayment(subscription);
      Customer customer = getOrCreateCustomer(subscription);

      ensureDefaultPaymentMethod(customer);

      SubscriptionCreateParams params = buildSubscriptionParams(subscription, customer, payment);
      com.stripe.model.Subscription.create(params);

      return "Success!";
    } catch (StripeException e) {
      log.error("Error creating Stripe subscription for subscription with id: {}, error message: {}", subscription.getId(), e.getMessage());
      throw new BusinessRuleException(SubscriptionValidationMessages.CREATE_STRIPE_SUBSCRIPTION_ERROR + subscription.getId());
    } catch (BusinessRuleException e) {
      log.error("Error creating Stripe subscription for subscription with id: {}, error message: {}", subscription.getId(), e.getMessage());
      throw e;
    }
  }

  private void ensureDefaultPaymentMethod(Customer customer) throws StripeException {
    if (customer.getDefaultSource() == null) {
      List<PaymentMethod> paymentMethods = PaymentMethod.list(
              PaymentMethodListParams.builder()
                      .setCustomer(customer.getId())
                      .build()
      ).getData();

      if (!paymentMethods.isEmpty()) {
        String paymentMethodId = paymentMethods.stream()
                .filter(pm -> "card".equals(pm.getType()))
                .map(PaymentMethod::getId)
                .findFirst()
                .orElse(paymentMethods.get(0).getId());
        updateCustomerDefaultSource(customer, paymentMethodId);
      } else {
        throw new BusinessRuleException("Nenhum método de pagamento encontrado para o cliente.");
      }
    }
  }

  private void updateCustomerDefaultSource(Customer stripeCustomer, String paymentMethod) {
    try {
      stripeCustomer.update(
              CustomerUpdateParams.builder()
                      .setInvoiceSettings(CustomerUpdateParams.InvoiceSettings.builder()
                              .setDefaultPaymentMethod(paymentMethod)
                              .build())
                      .build()
      );
      log.info("Método de pagamento com ID {} foi definido como padrão para o cliente com ID {}", paymentMethod, stripeCustomer.getId());
    } catch (StripeException e) {
      log.error("Error updating default payment method for customer with id: {}, error message: {}", stripeCustomer, e.getMessage());
      throw new BusinessRuleException(PaymentValidationMessages.PAYMENT_UPDATE_DEFAULT_SOURCE_ERROR + stripeCustomer);
    }
  }

  private Payment findEntityById(UUID paymentId) {
    return repository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException(PaymentValidationMessages.PAYMENT_NOT_FOUND));
  }

  private void finalizePayment(Payment payment) {
    log.info("Finalizing payment update with id: {} and status: {}, attached to subscription with id: {}", payment.getId(), payment.getStatus(), payment.getSubscription().getId());
    subscriptionRepository.save(payment.getSubscription());
    repository.save(payment);
  }

  private String generateSession(Subscription subscription, Payment payment, Recurrence recurrence) throws StripeException {
    Customer customer = getOrCreateCustomer(subscription);

    String baseURL = "http://localhost:4200";

    Map<String, String> metaData = new HashMap<>();
    metaData.put("subscriptionId", subscription.getId().toString());
    metaData.put("clientId", subscription.getClient().getId().toString());
    metaData.put("paymentId", payment.getId().toString());

    if (recurrence != null) {
      metaData.put("recurrence", recurrence.name());
    }

    String successUrl = baseURL + "/success?subscriptionId=" + subscription.getId() +
                        (subscription.getClient().getAds().isEmpty() ? "&ads=true" : "");

    boolean isSubscription = Recurrence.MONTHLY.equals(subscription.getRecurrence()) || (Recurrence.MONTHLY.equals(recurrence));

    SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
            .setMode(isSubscription ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
            .setCustomer(customer.getId())
            .setAdaptivePricing(SessionCreateParams.AdaptivePricing.builder()
                    .setEnabled(true)
                    .build())
            .setAllowPromotionCodes(false)
            .setConsentCollection(SessionCreateParams.ConsentCollection.builder()
                    .setPaymentMethodReuseAgreement(SessionCreateParams.ConsentCollection.PaymentMethodReuseAgreement.builder()
                            .setPosition(SessionCreateParams.ConsentCollection.PaymentMethodReuseAgreement.Position.AUTO)
                            .build())
                    .build())
            .setSuccessUrl(successUrl)
            .setCancelUrl(baseURL + "/")
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .setClientReferenceId(payment.getId().toString());


    if (isSubscription) {
      SessionCreateParams.SubscriptionData.Builder subscriptionDataBuilder = SessionCreateParams.SubscriptionData.builder()
              .putAllMetadata(metaData)
              .setDescription("Invoice payment for your tela's subscription");

      if (subscription.isUpgrade() && subscription.getEndsAt() != null) {
        long billingCycleAnchor = subscription.getEndsAt().getEpochSecond();
        subscriptionDataBuilder.setBillingCycleAnchor(billingCycleAnchor);
      }

      paramsBuilder.setSubscriptionData(subscriptionDataBuilder.build())
              .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
              .addLineItem(SessionCreateParams.LineItem.builder()
                      .setQuantity((long) subscription.getMonitors().size())
                      .setPrice(getProductPriceIdMonthly())
                      .build());
    } else {
      BigDecimal totalPrice = calculatePrice(subscription.getMonitors().size(), recurrence != null ? recurrence : subscription.getRecurrence());

      paramsBuilder.setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
              .setSetupFutureUsage(SessionCreateParams.PaymentIntentData.SetupFutureUsage.OFF_SESSION)
              .setReceiptEmail(customer.getEmail())
              .putAllMetadata(metaData)
              .build());

      paramsBuilder.addLineItem(
              SessionCreateParams.LineItem.builder()
                      .setQuantity(1L) // Sempre 1, pois o preço total já é calculado
                      .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                              .setCurrency("usd")
                              .setUnitAmount(totalPrice.multiply(BigDecimal.valueOf(100)).longValue())
                              .setProduct("prod_SP0KFP0uCSQrxt")
                              .build())
                      .build()
      );
    }
    Session session = Session.create(paramsBuilder.build());
    return session.getUrl();
  }

  private Payment savePayment(Subscription subscription) {
    Payment payment = new Payment(subscription);
    subscription.getPayments().add(payment);
    repository.save(payment);
    return payment;
  }

  private SubscriptionCreateParams buildSubscriptionParams(Subscription subscription, Customer customer, Payment payment) throws StripeException {
    Map<String, String> metaData = Map.of(
            "subscriptionId", subscription.getId().toString(),
            "clientId", subscription.getClient().getId().toString(),
            "paymentId", payment.getId().toString(),
            "recurrence", Recurrence.MONTHLY.name()
    );

    return SubscriptionCreateParams.builder()
            .setCustomer(customer.getId())
            .setCollectionMethod(SubscriptionCreateParams.CollectionMethod.CHARGE_AUTOMATICALLY)
//            .setDefaultPaymentMethod(customer.getDefaultSource())
            .setBillingCycleAnchor(subscription.getEndsAt().getEpochSecond())
            .setMetadata(metaData)
            .addItem(SubscriptionCreateParams.Item.builder()
                    .setPrice(getProductPriceIdMonthly())
                    .setQuantity((long) subscription.getMonitors().size())
                    .build())
            .build();
  }

  private BigDecimal calculatePrice(int quantity, Recurrence recurrence) {
    BigDecimal totalPrice = BigDecimal.ZERO;

    for (int i = 1; i <= quantity; i++) {
      BigDecimal unitPrice = (i == 1) ? BigDecimal.valueOf(700)
              : (i == 2) ? BigDecimal.valueOf(600)
              : BigDecimal.valueOf(500);
      totalPrice = MoneyUtils.add(totalPrice, unitPrice);
    }

    return totalPrice;
  }

  private void handleSuccessfulPayment(Payment payment, boolean isRecurringPayment) {
    log.info("Payment succeeded id: {}", payment.getId());
    Subscription subscription = payment.getSubscription();

    if (subscription.isUpgrade()) {
      log.info("Skipping cart inactivation for upgraded subscription: {}", subscription.getId());
      subscription.setUpgrade(false);
      return;
    }

    if (!isRecurringPayment) {
      Client client = subscription.getClient();
      subscriptionHelper.inactivateCart(client);
      subscriptionHelper.deleteSubscriptionFlow(client);
      clientRepository.save(client);
    }
  }

  private void handleFailedPayment(Payment payment) {
    log.error("Payment failed id: {}", payment.getId());
  }

  private void verifyClientOwnership(String stripeCustomerId, Payment payment) {
    Client client = findClientByStripeCustomerId(stripeCustomerId);

    // Verifica se o pagamento pertence ao cliente
    if (!payment.getSubscription().getClient().getId().equals(client.getId())) {
      throw new BusinessRuleException(PaymentValidationMessages.PAYMENT_CLIENT_MISMATCH + payment.getId() + " and client with id: " + client.getId());
    }
  }

  private Customer getOrCreateCustomer(Subscription subscription) throws StripeException {
    Client client = subscription.getClient();

    try {
      if (client.getStripeCustomerId() != null) {
        return Customer.retrieve(client.getStripeCustomerId());
      }

      String email = client.getContact().getEmail();
      CustomerSearchResult result = Customer.search(
              CustomerSearchParams.builder()
                      .setQuery("email:'" + email + "'")
                      .build()
      );

      Address clientAddress = client.getAddresses().stream().findFirst().orElse(null);

      Customer customer = result.getData().isEmpty()
              ? Customer.create(CustomerCreateParams.builder()
              .setEmail(email)
              .setName(client.getBusinessName())
              .setPhone(client.getContact().getPhone())
              .setAddress(CustomerCreateParams.Address.builder()
                      .setLine1(clientAddress != null ? clientAddress.getStreet() : null)
                      .setLine2(clientAddress != null ? clientAddress.getComplement() : null)
                      .setCity(clientAddress != null ? clientAddress.getCity() : null)
                      .setState(clientAddress != null ? clientAddress.getState() : null)
                      .setPostalCode(clientAddress != null ? clientAddress.getZipCode() : null)
                      .setCountry(clientAddress != null ? clientAddress.getCountry() : null)
                      .build())
              .build())
              : result.getData().get(0);

      client.setStripeCustomerId(customer.getId());
      clientRepository.save(client);

      return customer;
    } catch (StripeException e) {
      log.error("Error retrieving or creating customer: {}", e.getMessage());
      throw e;
    }
  }

  private Client findClientByStripeCustomerId(String stripeCustomerId) {
    return clientRepository.findByStripeCustomerId(stripeCustomerId)
            .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.CLIENT_CUSTOMER_NOT_FOUND + stripeCustomerId));
  }

  private String getProductPriceIdMonthly() throws StripeException {
    try {
      PriceListParams params = PriceListParams.builder()
              .setProduct("prod_SP0KFP0uCSQrxt")
              .addAllLookupKey(List.of("subscription"))
              .build();

      List<Price> prices = Price.list(params).getData();

      // Filtrar preços por recorrência mensal e avulso
      String monthlyPriceId = prices.stream()
              .filter(price -> price.getRecurring() != null && "month".equals(price.getRecurring().getInterval()))
              .map(Price::getId)
              .findFirst()
              .orElse(null);

      if (monthlyPriceId == null) {
        throw new ResourceNotFoundException(PaymentValidationMessages.PAYMENT_PRODUCT_PRICES_NOT_FOUND);
      }

      return monthlyPriceId;
    } catch (StripeException e) {
      log.error("Error retrieving monthly product price ID: {}", e.getMessage());
      throw new BusinessRuleException(PaymentValidationMessages.PAYMENT_PRODUCT_PRICES_NOT_FOUND);
    }
  }

  private Map<Recurrence, String> getProductPricesId() throws StripeException {
    try {
      PriceListParams params = PriceListParams.builder()
              .setProduct("prod_SP0KFP0uCSQrxt")
              .addAllLookupKey(List.of("subscription", "one_time", Recurrence.THIRTY_DAYS.name(), com.telas.enums.Recurrence.SIXTY_DAYS.name(), com.telas.enums.Recurrence.NINETY_DAYS.name()))
              .build();

      List<Price> prices = Price.list(params).getData();

      // Filtrar preços por recorrência mensal e avulso
      String monthlyPriceId = prices.stream()
              .filter(price -> price.getRecurring() != null && "month".equals(price.getRecurring().getInterval()))
              .map(Price::getId)
              .findFirst()
              .orElse(null);

      String thirtyDaysId = prices.stream()
              .filter(price -> "one_time".equals(price.getType()) && price.getLookupKey() != null && price.getLookupKey().equals(Recurrence.THIRTY_DAYS.name()))
              .map(Price::getId)
              .findFirst()
              .orElse(null);

      String sixtyDaysId = prices.stream()
              .filter(price -> "one_time".equals(price.getType()) && price.getLookupKey() != null && price.getLookupKey().equals(Recurrence.SIXTY_DAYS.name()))
              .map(Price::getId)
              .findFirst()
              .orElse(null);

      String ninetyDaysId = prices.stream()
              .filter(price -> "one_time".equals(price.getType()) && price.getLookupKey() != null && price.getLookupKey().equals(Recurrence.NINETY_DAYS.name()))
              .map(Price::getId)
              .findFirst()
              .orElse(null);

      if (monthlyPriceId == null || thirtyDaysId == null || sixtyDaysId == null || ninetyDaysId == null) {
        throw new ResourceNotFoundException(PaymentValidationMessages.PAYMENT_PRODUCT_PRICES_NOT_FOUND);
      }

      return Map.of(
              Recurrence.MONTHLY, monthlyPriceId,
              Recurrence.THIRTY_DAYS, thirtyDaysId,
              Recurrence.SIXTY_DAYS, sixtyDaysId,
              Recurrence.NINETY_DAYS, ninetyDaysId
      );

    } catch (StripeException e) {
      log.error("Error retrieving product prices: {}", e.getMessage());
      throw e;
    }
  }

  private void setPaymentMethod(PaymentIntent paymentIntent, Payment payment) throws StripeException {
    PaymentMethod paymentMethod = null;

    try {
      paymentMethod = PaymentMethod.retrieve(paymentIntent.getPaymentMethod());
      payment.setPaymentMethod(paymentMethod == null ? "unknown" : paymentMethod.getType().toLowerCase());
    } catch (StripeException e) {
      log.error("Error retrieving payment method with id: {}, error message: {}", paymentIntent.getPaymentMethod(), e.getMessage());
      throw e;
    }
  }

  private void setAuditInfo(Subscription subscription) {
    CustomRevisionListener.setUsername("Stripe Webhook");
    subscription.setUsernameUpdate("Stripe Webhook");
    subscription.getPayments().forEach(payment -> payment.setUsernameUpdate("Stripe Webhook"));
  }
}
