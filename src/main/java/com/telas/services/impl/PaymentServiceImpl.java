package com.telas.services.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
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
import com.telas.shared.constants.valitation.PaymentValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    Payment payment = getPaymentFromIntent(paymentIntent);

    if (payment == null) {
      return;
    }

    Subscription subscription = payment.getSubscription();

    if (subscriptionHelper.isRecurringPayment(subscription, paymentIntent)) {
      return;
    }

    subscriptionHelper.updateAuditInfo(subscription);

    if (!subscription.isUpgrade()) {
      subscription.setStatus(SubscriptionStatus.fromStripeStatus(paymentIntent.getStatus(), null, subscription));
    }

    subscriptionHelper.updatePaymentDetails(payment, paymentIntent);

    if (PaymentStatus.COMPLETED.equals(payment.getStatus())) {
      subscriptionHelper.handleCompletedPayment(subscription, paymentIntent);
    } else if (PaymentStatus.FAILED.equals(payment.getStatus())) {
      handleFailedPayment(payment);
    }

    finalizePayment(payment);
  }

  @Override
  @Transactional
  public void updatePaymentStatus(Invoice invoice) {
    Subscription subscription = subscriptionHelper.getSubscriptionFromInvoice(invoice);
    subscriptionHelper.updateAuditInfo(subscription);

    Payment payment = subscriptionHelper.getOrCreatePayment(invoice, subscription);

    subscriptionHelper.updatePaymentDetailsFromInvoice(payment, invoice);

    if (PaymentStatus.COMPLETED.equals(payment.getStatus())) {
      subscriptionHelper.handleCompletedPaymentFromInvoice(subscription, invoice);
    } else if (PaymentStatus.FAILED.equals(payment.getStatus())) {
      handleFailedPayment(payment);
    }

    finalizePayment(payment);
  }

  private String generateSession(Subscription subscription, Payment payment, Recurrence recurrence) throws StripeException {
    Customer customer = getOrCreateCustomer(subscription);
    String baseURL = "http://localhost:4200";
    Map<String, String> metaData = subscriptionHelper.createMetaData(subscription, payment, recurrence);

    String successUrl = baseURL + "/success?subscriptionId=" + subscription.getId() +
                        (subscription.getClient().getAds().isEmpty() ? "&ads=true" : "");

    boolean isSubscription = Recurrence.MONTHLY.equals(subscription.getRecurrence()) || Recurrence.MONTHLY.equals(recurrence);

    SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
            .setMode(isSubscription ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
            .setCustomer(customer.getId())
            .setSuccessUrl(successUrl)
            .setCancelUrl(baseURL + "/")
            .setClientReferenceId(payment.getId().toString());

    if (isSubscription) {
      subscriptionHelper.configureSubscriptionParams(paramsBuilder, subscription, metaData);
    } else {
      subscriptionHelper.configurePaymentParams(paramsBuilder, subscription, customer, metaData);
    }

    Session session = Session.create(paramsBuilder.build());
    return session.getUrl();
  }

  private void handleFailedPayment(Payment payment) {
    log.error("Payment failed id: {}", payment.getId());
  }

  private void finalizePayment(Payment payment) {
    log.info("Finalizing payment update with id: {} and status: {}, attached to subscription with id: {}", payment.getId(), payment.getStatus(), payment.getSubscription().getId());
    subscriptionRepository.save(payment.getSubscription());
    repository.save(payment);
  }

  private Customer getOrCreateCustomer(Subscription subscription) throws StripeException {
    Client client = subscription.getClient();

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
  }

  private Payment getPaymentFromIntent(PaymentIntent paymentIntent) {
    String paymentIdString = paymentIntent.getMetadata().get("paymentId");

    if (ValidateDataUtils.isNullOrEmptyString(paymentIdString)) {
      log.warn("Payment ID is missing in the metadata of the payment intent with id: {}", paymentIntent.getId());
      return null;
    }

    UUID paymentId = UUID.fromString(paymentIdString);
    return repository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException(PaymentValidationMessages.PAYMENT_NOT_FOUND));
  }
}