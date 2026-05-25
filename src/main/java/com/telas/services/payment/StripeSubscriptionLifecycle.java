package com.telas.services.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.billingportal.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.billingportal.SessionCreateParams;
import com.telas.entities.Address;
import com.telas.entities.Client;
import com.telas.entities.Subscription;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.shared.constants.valitation.SubscriptionValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StripeSubscriptionLifecycle {

    private final Logger log = LoggerFactory.getLogger(StripeSubscriptionLifecycle.class);
    private final ClientRepository clientRepository;
    private final SubscriptionRepository subscriptionRepository;
    @Value("${front.base.url}")
    private String frontBaseUrl;

    @Transactional
    public Customer getOrCreateCustomer(Subscription subscription) throws StripeException {
        return getOrCreateCustomer(subscription.getClient());
    }

    @Transactional
    public Customer getOrCreateCustomer(Client client) throws StripeException {
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
                ? createCustomer(client, clientAddress)
                : result.getData().get(0);

        client.setStripeCustomerId(customer.getId());
        clientRepository.save(client);
        return customer;
    }

    @Transactional(readOnly = true)
    public com.stripe.model.Subscription retrieveActiveStripeSubscription(Subscription subscription) throws StripeException {
        validateStripeId(subscription.getStripeId());

        com.stripe.model.Subscription stripeSubscription =
                com.stripe.model.Subscription.retrieve(subscription.getStripeId());
        validateStripeSubscription(stripeSubscription);

        return stripeSubscription;
    }

    @Transactional
    public void cancelImmediately(Subscription subscription) throws StripeException {
        com.stripe.model.Subscription stripeSubscription = retrieveActiveStripeSubscription(subscription);
        stripeSubscription.cancel();
    }

    @Transactional
    public void cancelImmediately(com.stripe.model.Subscription stripeSubscription) throws StripeException {
        stripeSubscription.cancel();
    }

    @Transactional
    public void applyCancelAtPeriodEnd(
            Subscription subscription,
            com.stripe.model.Subscription stripeSubscription) throws StripeException {
        stripeSubscription.update(Map.of("cancel_at_period_end", true));
        log.info("Subscription with id: {} set to cancel at the end of the billing period.", subscription.getId());
        subscription.setCancelAtPeriodEnd(true);
        subscription.setCancelRequestedAt(Instant.now());
        if (stripeSubscription.getCancelAt() != null) {
            Instant effectiveAt = Instant.ofEpochSecond(stripeSubscription.getCancelAt());
            subscription.setCancelAtPeriodEndAt(effectiveAt);
            subscription.setEndsAt(effectiveAt);
        }
    }

    @Transactional
    public String createCustomerPortalSession(Client client, String subscriptionsPath) throws StripeException {
        if (!subscriptionRepository.existsByClientId(client.getId())) {
            throw new ForbiddenException(SubscriptionValidationMessages.CLIENT_WITHOUT_SUBSCRIPTIONS);
        }

        if (ObjectUtils.isEmpty(client.getStripeCustomerId())) {
            throw new ForbiddenException(SubscriptionValidationMessages.CLIENT_WITHOUT_STRIPE_ID);
        }

        Customer customer = getOrCreateCustomer(client);
        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(customer.getId())
                .setReturnUrl(frontBaseUrl + subscriptionsPath)
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    private Customer createCustomer(Client client, Address address) throws StripeException {
        return Customer.create(CustomerCreateParams.builder()
                .setEmail(client.getContact().getEmail())
                .setName(client.getBusinessName())
                .setPhone(client.getContact().getPhone())
                .setAddress(CustomerCreateParams.Address.builder()
                        .setLine1(address != null ? address.getStreet() : null)
                        .setLine2(address != null ? address.getAddress2() : null)
                        .setCity(address != null ? address.getCity() : null)
                        .setState(address != null ? address.getState() : null)
                        .setPostalCode(address != null ? address.getZipCode() : null)
                        .setCountry(address != null ? address.getCountry() : null)
                        .build())
                .build());
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
            throw new BusinessRuleException(
                    SubscriptionValidationMessages.SUBSCRIPTION_NOT_ACTIVE_IN_STRIPE + stripeSubscription.getId());
        }
    }
}
