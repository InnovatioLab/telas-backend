package com.telas.services.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.telas.dtos.request.UpdatePaymentStatusRequestDto;
import com.telas.dtos.response.PaymentInfoResponseDto;
import com.telas.entities.Cart;
import com.telas.entities.Client;
import com.telas.entities.Payment;
import com.telas.entities.Subscription;
import com.telas.enums.PaymentStatus;
import com.telas.enums.Recurrence;
import com.telas.enums.SubscriptionStatus;
import com.telas.helpers.SubscriptionHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.PaymentRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.PaymentService;
import com.telas.shared.constants.valitation.PaymentValidationMessages;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
  private final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
  private final PaymentRepository repository;
  private final SubscriptionRepository subscriptionRepository;
  private final ClientRepository clientRepository;
  private final AuthenticatedUserService authenticatedUserService;
  private final SubscriptionHelper subscriptionHelper;

  @Value("${PAYMENT_GATEWAY_API_KEY}")
  private String key;

  @Override
  @Transactional
  public PaymentInfoResponseDto process(Subscription subscription) {
    Stripe.apiKey = key;

    Customer customer = getOrCreateCustomer(subscription);

    Payment payment = new Payment(subscription);
    subscription.setPayment(payment);

    if (Recurrence.MONTHLY.equals(subscription.getRecurrence())) {
      repository.save(payment);
      return generateRecurringPayment(subscription, customer);
    }

    PaymentIntent paymentIntent = generatePaymentIntent(subscription, customer);
    payment.setStripePaymentId(paymentIntent.getId());

    repository.save(payment);
    subscriptionRepository.save(subscription);

    return new PaymentInfoResponseDto(paymentIntent);
  }

  @Override
  @Transactional
  public void updatePaymentStatus(UpdatePaymentStatusRequestDto request) {
    Payment payment = findPaymentByStripeId(request.getId());
    Client client = authenticatedUserService.getLoggedUser().client();
    PaymentStatus paymentStatus = PaymentStatus.fromStripeStatus(request.getStatus());

    verifyClientOwnership(payment, client);

    if (PaymentStatus.COMPLETED.equals(paymentStatus)) {
      handleSuccessfulPayment(payment);
    } else if (PaymentStatus.FAILED.equals(paymentStatus)) {
      handleFailedPayment(payment);
    }

    finalizePayment(payment, client);
  }

  private void handleSuccessfulPayment(Payment payment) {
    log.info("Payment succeeded id: {}", payment.getId());
    payment.setStatus(PaymentStatus.COMPLETED);

    Subscription subscription = payment.getSubscription();
    log.info("Initializing subscription id: {}", subscription.getId());
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.initialize();
  }

  private void handleFailedPayment(Payment payment) {
    log.error("Payment failed id: {}", payment.getId());
    payment.setStatus(PaymentStatus.FAILED);
  }

  private void finalizePayment(Payment payment, Client client) {
    repository.save(payment);
    Subscription subscription = payment.getSubscription();
    subscriptionRepository.save(subscription);

    Cart cart = subscriptionHelper.getActiveCart(client);
    subscriptionHelper.inactivateCart(cart);
  }

  private void verifyClientOwnership(Payment payment, Client client) {
    if (!payment.getSubscription().getClient().getId().equals(client.getId())) {
      throw new BusinessRuleException(PaymentValidationMessages.PAYMENT_NOT_BELONG_TO_CLIENT);
    }
  }

  private Customer getOrCreateCustomer(Subscription subscription) {
    Client client = subscription.getClient();
    Customer customer;

    try {
      if (client.getStripeCustomerId() != null) {
        customer = Customer.retrieve(client.getStripeCustomerId());
      } else {
        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setEmail(client.getContact().getEmail())
                .setName(client.getBusinessName())
                .build();

        customer = Customer.create(customerParams);

        client.setStripeCustomerId(customer.getId());
        clientRepository.save(client);
      }

      return customer;
    } catch (StripeException e) {
      log.error("Error retrieving or creating customer: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falha ao recuperar customer!");
    }
  }

  private PaymentInfoResponseDto generateRecurringPayment(Subscription subscriptionEntity, Customer customer) {

    try {
      // Criar uma assinatura para o cliente
      SubscriptionCreateParams subscriptionParams = SubscriptionCreateParams.builder()
              .setCustomer(customer.getId())
              .addItem(
                      SubscriptionCreateParams.Item.builder()
                              .setPrice("price_12345") // ID do preço configurado no Stripe
                              .build()
              )
              .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
              .build();

      com.stripe.model.Subscription subscription = com.stripe.model.Subscription.create(subscriptionParams);
//        Stripe irá responder com: customerId, subscriptionId, paymentMethod

      // Retorne o status da assinatura
      String subscriptionStatus = subscription.getStatus();
      log.info("Subscription status: {}", subscriptionStatus);
//    return subscriptionStatus;
      return null;
    } catch (StripeException e) {
      log.error("Error creating stripe subscription: {}", e.getMessage());
      throw new BusinessRuleException(PaymentValidationMessages.SUBSCRIPTION_ERROR);
    }
  }

  public PaymentIntent generatePaymentIntent(Subscription subscription, Customer customer) {
    PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            // Valor em centavos
//            .setAmount(subscription.getAmount().multiply(BigDecimal.valueOf(1)).longValue()) 
            .setAmount(100L)
            .setCurrency(subscription.getPayment().getCurrency().name().toLowerCase())
            .setCustomer(customer.getId())
            .setDescription("Telas Payment")
            .build();

    try {
      return PaymentIntent.create(params);
    } catch (StripeException e) {
      log.error("Error creating payment intent: {}", e.getMessage());
      throw new BusinessRuleException(PaymentValidationMessages.PAYMENT_INTENT_ERROR);
    }
  }

  private Payment findPaymentByStripeId(String stripeId) {
    return repository.findByStripePaymentId(stripeId).orElseThrow(() -> new ResourceNotFoundException(PaymentValidationMessages.PAYMENT_NOT_FOUND));
  }
}
