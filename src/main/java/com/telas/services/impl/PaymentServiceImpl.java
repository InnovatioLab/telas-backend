package com.telas.services.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.telas.entities.*;
import com.telas.entities.Address;
import com.telas.entities.Subscription;
import com.telas.enums.PaymentStatus;
import com.telas.enums.Recurrence;
import com.telas.enums.SubscriptionStatus;
import com.telas.helpers.SubscriptionHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.PaymentRepository;
import com.telas.repositories.SubscriptionFlowRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.PaymentService;
import com.telas.shared.constants.valitation.PaymentValidationMessages;
import com.telas.shared.utils.MoneyUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
  private final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
  private final PaymentRepository repository;
  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionFlowRepository subscriptionFlowRepository;
  private final ClientRepository clientRepository;
  private final SubscriptionHelper subscriptionHelper;

  @Value("${PAYMENT_GATEWAY_API_KEY}")
  private String key;

  @Override
  @Transactional
  public String process(Subscription subscription) {
    try {
      Stripe.apiKey = key;
      Payment payment = new Payment(subscription);
      subscription.getPayments().add(payment);
      repository.save(payment);
      return generateSession(subscription, payment);
    } catch (StripeException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @Transactional
  public void updatePaymentStatus(PaymentIntent paymentIntent) throws StripeException {
    UUID paymentId = UUID.fromString(paymentIntent.getMetadata().get("paymentId"));
    Payment payment = findEntityById(paymentId);

    if (Recurrence.MONTHLY.equals(payment.getSubscription().getRecurrence())) {
      return; // Não processa pagamentos recorrentes aqui
    }

    verifyClientOwnership(paymentIntent.getCustomer(), payment);
    setPaymentMethod(paymentIntent, payment);

    PaymentStatus paymentStatus = PaymentStatus.fromStripeStatus(paymentIntent.getStatus(), null, payment);
    payment.setStatus(paymentStatus);
    payment.setStripePaymentId(paymentIntent.getId());

    Subscription subscription = payment.getSubscription();
    SubscriptionStatus subscriptionStatus = SubscriptionStatus.fromStripeStatus(paymentIntent.getStatus(), null, subscription);
    subscription.setStatus(subscriptionStatus);

    BigDecimal amountReceived = paymentIntent.getAmountReceived() != null
            ? MoneyUtils.divide(BigDecimal.valueOf(paymentIntent.getAmountReceived()), BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

    BigDecimal amountCharged = paymentIntent.getAmount() != null
            ? MoneyUtils.divide(BigDecimal.valueOf(paymentIntent.getAmount()), BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

    payment.setAmount(amountCharged);

    if (PaymentStatus.COMPLETED.equals(paymentStatus)) {
      subscription.setAmount(amountReceived);
      subscription.initialize();
      handleSuccessfulPayment(payment, false);
    } else if (PaymentStatus.FAILED.equals(paymentStatus)) {
      handleFailedPayment(payment);
    }

    finalizePayment(payment);
  }

  @Override
  @Transactional
  public void updatePaymentStatus(Invoice invoice) {
    boolean isRecurringPayment = invoice.getBillingReason() != null && invoice.getBillingReason().equals("subscription_cycle");
    UUID subscriptionId = UUID.fromString(invoice.getParent().getSubscriptionDetails().getMetadata().get("subscriptionId"));
    Subscription subscription = subscriptionHelper.findEntityById(subscriptionId);

    Payment payment;
    if (!isRecurringPayment) {
      payment = subscription.getPayments().stream()
              .findFirst()
              .orElseGet(() -> new Payment(subscription));
    } else {
      payment = new Payment(subscription);
    }

    subscription.getPayments().add(payment);
    payment.setStripePaymentId(invoice.getId());
    verifyClientOwnership(invoice.getCustomer(), payment);

    PaymentStatus paymentStatus = PaymentStatus.fromStripeStatus(null, invoice.getStatus(), payment);
    payment.setStatus(paymentStatus);
    subscriptionHelper.updateSubscriptionPeriod(invoice, subscription);

    SubscriptionStatus subscriptionStatus = SubscriptionStatus.fromStripeStatus(null, invoice.getStatus(), subscription);
    subscription.setStatus(subscriptionStatus);

    BigDecimal amountPaid = MoneyUtils.divide(BigDecimal.valueOf(
            invoice.getAmountPaid() != null ? invoice.getAmountPaid() : 0), BigDecimal.valueOf(100));

    if (BigDecimal.ZERO.compareTo(subscription.getAmount()) == 0) {
      subscription.setAmount(amountPaid);
    }

    BigDecimal amountDue = invoice.getAmountDue() != null
            ? MoneyUtils.divide(BigDecimal.valueOf(invoice.getAmountDue()), BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
    payment.setAmount(amountDue);

    if (PaymentStatus.COMPLETED.equals(paymentStatus)) {
      handleSuccessfulPayment(payment, isRecurringPayment);
    } else if (PaymentStatus.FAILED.equals(paymentStatus)) {
      handleFailedPayment(payment);
    }

    finalizePayment(payment);
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

  private String generateSession(Subscription subscription, Payment payment) throws StripeException {
    Customer customer = getOrCreateCustomer(subscription);

    String baseURL = "http://localhost:4200";

    Map<String, String> metaData = Map.of(
            "subscriptionId", subscription.getId().toString(),
            "clientId", subscription.getClient().getId().toString(),
            "paymentId", payment.getId().toString()
    );

    SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
            .setMode(Recurrence.MONTHLY.equals(subscription.getRecurrence()) ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
            .setCustomer(customer.getId())
            .setSuccessUrl(baseURL + "/success")
            .setClientReferenceId(payment.getId().toString())
            .setCancelUrl(baseURL + "/failure");

    if (!Recurrence.MONTHLY.equals(subscription.getRecurrence())) {
      paramsBuilder.setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
              .setCaptureMethod(SessionCreateParams.PaymentIntentData.CaptureMethod.AUTOMATIC)
              .putAllMetadata(metaData)
              .build());
    } else {
      paramsBuilder
              .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                      .putAllMetadata(metaData)
                      .setDescription("Invoice payment for your tela's subscription")
                      .build());
    }

    String priceKey = Recurrence.MONTHLY.equals(subscription.getRecurrence()) ? "monthlyPrice" : "oneTimePrice";
    String priceId = getProductPricesId().get(priceKey);

    paramsBuilder.addLineItem(
            SessionCreateParams.LineItem.builder()
                    .setQuantity((long) subscription.getMonitors().size())
                    .setPrice(priceId)
                    .build()
    );

    Session session = Session.create(paramsBuilder.build());
    return session.getUrl();
  }

  private void handleSuccessfulPayment(Payment payment, boolean isRecurringPayment) {
    log.info("Payment succeeded id: {}", payment.getId());
    Subscription subscription = payment.getSubscription();

    if (isRecurringPayment) {
      return;
    }

    Cart cart = payment.getSubscription().getClient().getCart();
    subscriptionHelper.deleteCart(cart);
    subscriptionFlowRepository.delete(subscription.getClient().getSubscriptionFlow());
  }

  private void handleFailedPayment(Payment payment) {
    log.error("Payment failed id: {}", payment.getId());
  }

  private void verifyClientOwnership(String stripeCustomerId, Payment payment) {
    Client client = findClientByStripeCustomerId(stripeCustomerId);

    // Verifica se o pagamento pertence ao cliente
    if (!payment.getSubscription().getClient().getId().equals(client.getId())) {
      throw new BusinessRuleException("O pagamento não pertence ao cliente especificado.");
    }
  }

  private Customer getOrCreateCustomer(Subscription subscription) {
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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falha ao recuperar customer!");
    }
  }

  private Client findClientByStripeCustomerId(String stripeCustomerId) {
    return clientRepository.findByStripeCustomerId(stripeCustomerId)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o stripeCustomerId: " + stripeCustomerId));
  }

  private Map<String, String> getProductPricesId() {
    try {
      PriceListParams params = PriceListParams.builder()
              .setProduct("prod_SP0KFP0uCSQrxt")
              .addAllLookupKey(List.of("subscription", "one_time"))
              .build();

      List<Price> prices = Price.list(params).getData();

      // Filtrar preços por recorrência mensal e avulso
      String monthlyPriceId = prices.stream()
              .filter(price -> price.getRecurring() != null && "month".equals(price.getRecurring().getInterval()))
              .map(Price::getId)
              .findFirst()
              .orElse(null);

      String oneTimePriceId = prices.stream()
              .filter(price -> "one_time".equals(price.getType()))
              .map(Price::getId)
              .findFirst()
              .orElse(null);

      if (monthlyPriceId == null || oneTimePriceId == null) {
        throw new ResourceNotFoundException("Preços não encontrados para o produto: " + "prod_SP0KFP0uCSQrxt");
      }

      return Map.of(
              "monthlyPrice", monthlyPriceId,
              "oneTimePrice", oneTimePriceId
      );
    } catch (StripeException e) {
      throw new RuntimeException("Erro ao buscar preços do produto: " + e.getMessage(), e);
    }
  }

  private void setPaymentMethod(PaymentIntent paymentIntent, Payment payment) {
    PaymentMethod paymentMethod = null;

    try {
      paymentMethod = PaymentMethod.retrieve(paymentIntent.getPaymentMethod());
      payment.setPaymentMethod(paymentMethod == null ? "unknown" : paymentMethod.getType().toLowerCase());
    } catch (StripeException e) {
      log.error("Error retrieving payment method with id: {}, error message: {}", paymentIntent.getPaymentMethod(), e.getMessage());
      throw new BusinessRuleException("Erro ao recuperar método de pagamento: " + e.getMessage());
    }

  }
}
