package com.telas.services.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
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
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
  private final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
  private final PaymentRepository repository;
  private final SubscriptionFlowRepository subscriptionFlowRepository;
  private final ClientRepository clientRepository;
  private final SubscriptionHelper subscriptionHelper;

  @Value("${PAYMENT_GATEWAY_API_KEY}")
  private String key;

  private static void setPaymentMethod(PaymentIntent paymentIntent, Payment payment) {
    PaymentMethod paymentMethod = paymentIntent.getPaymentMethodObject();
    payment.setPaymentMethod(paymentMethod != null ? paymentMethod.getType().toLowerCase() : "unknown");
  }

  @Override
  @Transactional
  public String process(Subscription subscription) {
    Stripe.apiKey = key;

    Customer customer = getOrCreateCustomer(subscription);
    Payment payment = new Payment(subscription);
    subscription.setPayment(payment);

    if (Recurrence.MONTHLY.equals(subscription.getRecurrence())) {
      repository.save(payment);
      return generateRecurringPayment(subscription, customer, payment);
    }

    PaymentIntent paymentIntent = generatePaymentIntent(subscription, customer);
    payment.setStripePaymentId(paymentIntent.getId());

    repository.save(payment);

    return paymentIntent.getClientSecret();
  }

  @Override
  @Transactional
  public void updatePaymentStatus(PaymentIntent paymentIntent) {
    Payment payment = findPaymentByStripeId(paymentIntent.getId());
    verifyClientOwnership(paymentIntent.getCustomer(), payment);

    PaymentStatus paymentStatus = PaymentStatus.fromStripeStatus(paymentIntent.getStatus(), null, payment);
    setPaymentMethod(paymentIntent, payment);
    payment.setStatus(paymentStatus);

    Subscription subscription = payment.getSubscription();
    SubscriptionStatus subscriptionStatus = SubscriptionStatus.fromStripeStatus(paymentIntent.getStatus(), null);
    subscription.setStatus(subscriptionStatus);

    if (PaymentStatus.COMPLETED.equals(paymentStatus)) {
      handleSuccessfulPayment(payment, false);
    } else if (PaymentStatus.FAILED.equals(paymentStatus)) {
      handleFailedPayment(payment);
    }

    repository.save(payment);
  }

  @Override
  @Transactional
  public void updatePaymentStatus(Invoice invoice) {
//    PaymentIntent paymentIntent = invoice.getPaymentIntentObject();
//
//    Payment payment = findPaymentByStripeId(invoice.getId());
//    verifyClientOwnership(invoice.getCustomer(), payment);
//
//    PaymentStatus paymentStatus = PaymentStatus.fromStripeStatus(paymentIntent.getStatus(), invoice.getSubscriptionObject().getStatus(), payment);
//    payment.setStatus(paymentStatus);
//    setPaymentMethod(paymentIntent, payment);
//
//    Subscription subscription = payment.getSubscription();
//    updateSubscriptionPeriod(invoice, subscription);
//
//    SubscriptionStatus subscriptionStatus = SubscriptionStatus.fromStripeStatus(paymentIntent.getStatus(), invoice.getSubscriptionObject().getStatus());
//    subscription.setStatus(subscriptionStatus);
//
//    boolean isRecurringPayment = invoice.getBillingReason() != null && invoice.getBillingReason().equals("subscription_cycle");
//
//    if (PaymentStatus.COMPLETED.equals(paymentStatus)) {
//      handleSuccessfulPayment(payment, isRecurringPayment);
//    } else if (PaymentStatus.FAILED.equals(paymentStatus)) {
//      handleFailedPayment(payment);
//    }
//
//    repository.save(payment);
  }

  private void updateSubscriptionPeriod(Invoice invoice, Subscription subscription) {
    if (invoice.getPeriodStart() != null && invoice.getPeriodEnd() != null) {
      subscription.setStartedAt(Instant.ofEpochSecond(invoice.getPeriodStart()));
      subscription.setEndsAt(Instant.ofEpochSecond(invoice.getPeriodEnd()));
    }
  }

  private void handleSuccessfulPayment(Payment payment, boolean isRecurringPayment) {
    log.info("Payment succeeded id: {}", payment.getId());
    Subscription subscription = payment.getSubscription();

    if (isRecurringPayment) {
      return;
    }

    Cart cart = payment.getSubscription().getClient().getCart();
    subscriptionHelper.inactivateCart(cart);
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

  private String generateRecurringPayment(Subscription subscriptionEntity, Customer customer, Payment payment) {
    try {

      PriceCreateParams priceParams = PriceCreateParams.builder()
              .setUnitAmount(subscriptionEntity.getAmount().multiply(BigDecimal.valueOf(100)).longValue()) // Valor em centavos
              .setCurrency("usd")
              .setRecurring(
                      PriceCreateParams.Recurring.builder()
                              .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                              .build()
              )
              .setProduct("prod_SM3FkDpPVvQpGn") // ID do produto configurado no Stripe
              .build();

      Price price = Price.create(priceParams);

      // Criar uma assinatura para o cliente
      SubscriptionCreateParams subscriptionParams = SubscriptionCreateParams.builder()
              .setCustomer(customer.getId())
              .addItem(
                      SubscriptionCreateParams.Item.builder()
                              .setPrice(price.getId())
                              .build()
              )
              .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
              .setAutomaticTax(SubscriptionCreateParams.AutomaticTax.builder().setEnabled(true).build())
              .build();

      com.stripe.model.Subscription subscription = com.stripe.model.Subscription.create(subscriptionParams);

      Invoice invoice = Invoice.retrieve(subscription.getLatestInvoice());
      payment.setStripePaymentId(subscription.getId());

      return invoice.getConfirmationSecret().getClientSecret();
    } catch (StripeException e) {
      log.error("Error creating stripe subscription: {}", e.getMessage());
      throw new BusinessRuleException(PaymentValidationMessages.SUBSCRIPTION_ERROR);
    }
  }

  private PaymentIntent generatePaymentIntent(Subscription subscription, Customer customer) {
    long amount = calculateAmount(subscription);
    PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amount)
            .setCurrency("usd")
            .setCustomer(customer.getId())
            .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods
                            .builder()
                            .setEnabled(true)
                            .build()
            )
            .setDescription("Telas Payment")
            .setReceiptEmail(customer.getEmail())
            .build();

    try {
      return PaymentIntent.create(params);
    } catch (StripeException e) {
      log.error("Error creating payment intent: {}", e.getMessage());
      throw new BusinessRuleException(PaymentValidationMessages.PAYMENT_INTENT_ERROR);
    }
  }

  private long calculateAmount(Subscription subscription) {
    BigDecimal amount = MoneyUtils.subtract(subscription.getAmount(), subscription.getDiscount());

    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessRuleException(PaymentValidationMessages.PAYMENT_AMOUNT_NEGATIVE);
    }

    return amount.multiply(BigDecimal.valueOf(100)).longValue();
  }

  private Payment findPaymentByStripeId(String stripeId) {
    return repository.findByStripePaymentId(stripeId).orElseThrow(() -> new ResourceNotFoundException(PaymentValidationMessages.PAYMENT_NOT_FOUND));
  }

  private Client findClientByStripeCustomerId(String stripeCustomerId) {
    return clientRepository.findByStripeCustomerId(stripeCustomerId)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o stripeCustomerId: " + stripeCustomerId));
  }
}
