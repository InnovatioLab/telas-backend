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
import com.telas.helpers.PaymentHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.PaymentRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.PaymentService;
import com.telas.shared.constants.valitation.PaymentValidationMessages;
import com.telas.shared.constants.valitation.SubscriptionValidationMessages;
import com.telas.shared.utils.MoneyUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final PaymentRepository repository;
    private final SubscriptionRepository subscriptionRepository;
    private final ClientRepository clientRepository;
    private final PaymentHelper helper;

    @Value("${front.base.url}")
    private String frontBaseUrl;

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

        if (helper.isRecurringPayment(subscription, paymentIntent)) {
            return;
        }

        helper.updateAuditInfo(subscription);

        if (Objects.isNull(subscription.getStartedAt())) {
            subscription.setStatus(SubscriptionStatus.fromStripeStatus(paymentIntent.getStatus(), null, subscription));
        }

        helper.updatePaymentDetails(payment, paymentIntent);

        if (PaymentStatus.COMPLETED.equals(payment.getStatus())) {
            helper.handleCompletedPayment(subscription, payment, paymentIntent);
        } else if (PaymentStatus.FAILED.equals(payment.getStatus())) {
            handleFailedPayment(payment);
        }

        finalizePayment(payment);
    }

    @Override
    @Transactional
    public void updatePaymentStatus(Invoice invoice) {
        Subscription subscription = helper.getSubscriptionFromInvoice(invoice);
        helper.updateAuditInfo(subscription);

        Payment payment = helper.getOrCreatePayment(invoice, subscription);

        if (payment.getId() == null) {
            repository.save(payment);
        }

        helper.updatePaymentDetailsFromInvoice(payment, invoice);

        if (!subscription.isUpgrade()) {
            subscription.setStatus(SubscriptionStatus.fromStripeStatus(null, invoice.getStatus(), subscription));
        }

        if (PaymentStatus.COMPLETED.equals(payment.getStatus())) {
            helper.handleCompletedPaymentFromInvoice(subscription, payment, invoice);
        } else if (PaymentStatus.FAILED.equals(payment.getStatus())) {
            handleFailedPayment(payment);
        }

        finalizePayment(payment);
    }

    @Override
    @Transactional
    public void handleDisputeFundsWithdrawn(com.stripe.model.Dispute dispute) {
        String paymentIntentId = dispute.getPaymentIntent();

        if (ValidateDataUtils.isNullOrEmptyString(paymentIntentId)) {
            log.warn("Dispute without paymentIntentId.");
            return;
        }

        Payment payment = getPaymentFromStripeId(paymentIntentId);

        BigDecimal disputeAmount = dispute.getAmount() != null
                ? BigDecimal.valueOf(dispute.getAmount() / 100.0)
                : BigDecimal.ZERO;

        payment.setAmount(MoneyUtils.subtract(payment.getAmount(), disputeAmount));
        payment.setStatus(PaymentStatus.FAILED);
        repository.save(payment);

        Subscription subscription = payment.getSubscription();

        if (subscription == null || !SubscriptionStatus.ACTIVE.equals(subscription.getStatus())) {
            log.warn("Subscription not found or not active for payment with id: {}", payment.getId());
            return;
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setEndsAt(Instant.now());
        subscriptionRepository.save(subscription);

        if (Recurrence.MONTHLY.equals(subscription.getRecurrence())) {
            log.info("Cancelling Stripe subscription with id: {} due to dispute funds withdrawn.", subscription.getId());
            cancelStripeSubscription(subscription);
        }
    }

    private void cancelStripeSubscription(Subscription subscription) {
        try {
            com.stripe.model.Subscription stripeSubscription = helper.getStripeSubscription(subscription);
            stripeSubscription.cancel();
        } catch (StripeException e) {
            log.error("Error cancelling subscription with id: {} on Stripe, error message: {}", subscription.getId(), e.getMessage());
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_CANCELLATION_ERROR_DURING_DISPUTE + subscription.getId());
        }
    }

    private Payment getPaymentFromStripeId(String paymentIntentId) {
        return repository.findByStripeId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException(PaymentValidationMessages.PAYMENT_NOT_FOUND));
    }

    private String generateSession(Subscription subscription, Payment payment, Recurrence recurrence) throws StripeException {
        Customer customer = getOrCreateCustomer(subscription);
        Map<String, String> metaData = helper.createMetaData(subscription, payment, recurrence);

        String successUrl = Objects.isNull(recurrence) ? helper.getSuccessUrl(subscription.getClient()) : (frontBaseUrl + "/client/subscriptions");

        boolean isSubscription = Recurrence.MONTHLY.equals(subscription.getRecurrence()) || Recurrence.MONTHLY.equals(recurrence);

        long expiresAt = (System.currentTimeMillis() / 1000L) + 1800;

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(isSubscription ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
                .setCustomer(customer.getId())
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setSuccessUrl(successUrl)
                .setExpiresAt(expiresAt)
                .setCancelUrl(frontBaseUrl + "/client")
                .setClientReferenceId(subscription.getClient().getId().toString());

        if (isSubscription) {
            helper.configureSubscriptionParams(paramsBuilder, subscription, metaData, recurrence);
        } else {
            helper.configurePaymentParams(paramsBuilder, subscription, customer, metaData, recurrence);
        }

        Session session = Session.create(paramsBuilder.build());
        return session.getUrl();
    }

    private void handleFailedPayment(Payment payment) {
        log.warn("Payment failed id: {}", payment.getId());
    }

    private void finalizePayment(Payment payment) {
        log.info("Finalizing payment update with id: {} and status: {}, attached to subscription with id: {}", payment.getId(), payment.getStatus(), payment.getSubscription().getId());
        subscriptionRepository.save(payment.getSubscription());
        repository.save(payment);
    }

    private Customer getOrCreateCustomer(Subscription subscription) throws StripeException {
        Client client = subscription.getClient();

        if (Objects.nonNull(client.getStripeCustomerId())) {
            try {
                return Customer.retrieve(client.getStripeCustomerId());
            } catch (StripeException e) {
                log.warn("Failed to retrieve Customer from Stripe, creating new one.");
            }
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