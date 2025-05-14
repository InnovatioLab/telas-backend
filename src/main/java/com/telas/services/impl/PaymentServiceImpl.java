package com.telas.services.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.telas.entities.Payment;
import com.telas.entities.Subscription;
import com.telas.enums.PaymentStatus;
import com.telas.enums.Recurrence;
import com.telas.repositories.PaymentRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final PaymentRepository repository;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${PAYMENT_GATEWAY_API_KEY}")
    private String key;

    @Override
    @Transactional
    public String process(Subscription subscription) throws StripeException {
        //        StripeRequest vai precisar: productName, amount, quantity, currency.
//        Vai retornar: sessionId, sessionUrl, status, message.
        Stripe.apiKey = key;

        // Criar customer aqui

        Payment payment = new Payment();
        payment.setAmount(subscription.getAmount());
        payment.setCurrency(subscription.getPayment().getCurrency());
        payment.setPaymentMethod("stripe");
        payment.setSubscription(subscription);

        subscription.setPayment(payment);

        if (Recurrence.MONTHLY.equals(subscription.getRecurrence())) {
            repository.save(payment);
            return generateRecurringPayment(subscription);
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(subscription.getAmount().multiply(BigDecimal.valueOf(1)).longValue()) // Valor em centavos
                .setCurrency(subscription.getPayment().getCurrency().name().toLowerCase())
                .setCustomer(subscription.getClient().getStripeCustomerId())
                .setDescription("Telas Payment")
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        // Verificar o status do PaymentIntent
        String paymentStatus = paymentIntent.getStatus();

        if ("succeeded".equals(paymentStatus)) {
            log.info("Payment succeeded for subscription id: {}", subscription.getId());
            payment.setStatus(PaymentStatus.COMPLETED);
            log.info("initializing subscription with id: {}", subscription.getId());
            subscription.initialize();
        } else if ("requires_payment_method".equals(paymentStatus)) {
            log.error("Payment failed: {}, with subscription id: {}", paymentIntent.getLastPaymentError(), subscription.getId());
            payment.setStatus(PaymentStatus.FAILED);
        } else {
            payment.setStatus(PaymentStatus.PENDING);
        }

        // Atualizar o Payment com o ID do Stripe e status
        payment.setStripePaymentId(paymentIntent.getId());

        repository.save(payment);
        subscriptionRepository.save(subscription);
        return paymentIntent.getClientSecret();
    }

    private String generateRecurringPayment(Subscription subscriptionEntity) throws StripeException {
        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setEmail(subscriptionEntity.getClient().getContact().getEmail())
                .setName(subscriptionEntity.getClient().getBusinessName())
                .build();

        Customer customer = Customer.create(customerParams);

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
        return subscriptionStatus;
    }
}
