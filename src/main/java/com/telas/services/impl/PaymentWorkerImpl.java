package com.telas.services.impl;

import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.telas.services.PaymentService;
import com.telas.services.PaymentWorker;
import com.telas.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentWorkerImpl implements PaymentWorker {
  private final Logger log = LoggerFactory.getLogger(PaymentWorkerImpl.class);
  private final PaymentService paymentService;
  private final SubscriptionService subscriptionService;

  @Override
  @RabbitListener(queues = "${queue.name}")
  public void processEvent(String eventJson) {
    Event event = null;

    try {
      event = Event.GSON.fromJson(eventJson, Event.class);
      log.info("[WORKER]: Processing event with ID: {} and type: {}", event.getId(), event.getType());

      switch (event.getType()) {
        case "checkout.session.expired":
          handleCheckoutSessionExpired(event);
          break;
        case "charge.dispute.funds_withdrawn":
          handleChargeDisputeFundsWithdrawn(event);
          break;
        case "invoice.payment_failed":
        case "invoice.payment_succeeded":
          handleInvoicePayment(event);
          break;
        case "payment_intent.succeeded":
        case "payment_intent.canceled":
        case "payment_intent.payment_failed":
          handlePaymentIntent(event);
          break;
        case "customer.subscription.deleted":
          handleSubscriptionDeleted(event);
          break;
        default:
          break;
      }
    } catch (Exception e) {
      log.error("Error during worker event processing, event with ID {}: {}", event != null ? event.getId() : "unknown", e.getMessage());
      throw e;
    }
  }

  private void handleCheckoutSessionExpired(Event event) {
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

    if (dataObjectDeserializer.getObject().isPresent()) {
      StripeObject stripeObject = dataObjectDeserializer.getObject().get();

      if (stripeObject instanceof Session session) {
        log.info("[WORKER]: Handling expired checkout session with ID: {}", session.getId());
        subscriptionService.handleCheckoutSessionExpired(session);
      }
    }
  }

  private void handlePaymentIntent(Event event) {
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

    if (dataObjectDeserializer.getObject().isPresent()) {
      StripeObject stripeObject = dataObjectDeserializer.getObject().get();

      if (stripeObject instanceof PaymentIntent paymentIntent) {
        log.info("[WORKER]: Handling payment intent with ID: {}", paymentIntent.getId());
        paymentService.updatePaymentStatus(paymentIntent);
      }
    }
  }

  private void handleInvoicePayment(Event event) {
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

    if (dataObjectDeserializer.getObject().isPresent()) {
      StripeObject stripeObject = dataObjectDeserializer.getObject().get();

      if (stripeObject instanceof Invoice invoice) {
        log.info("[WORKER]: Handling invoice payment with ID: {}", invoice.getId());
        paymentService.updatePaymentStatus(invoice);
      }
    }
  }

  private void handleSubscriptionDeleted(Event event) {
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

    if (dataObjectDeserializer.getObject().isPresent()) {
      StripeObject stripeObject = dataObjectDeserializer.getObject().get();

      if (stripeObject instanceof Subscription subscription) {
        log.info("[WORKER]: Handling subscription deletion with ID: {}", subscription.getId());
        subscriptionService.cancelSubscription(subscription);
      }
    }
  }

  private void handleChargeDisputeFundsWithdrawn(Event event) {
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

    if (dataObjectDeserializer.getObject().isPresent()) {
      StripeObject stripeObject = dataObjectDeserializer.getObject().get();

      if (stripeObject instanceof com.stripe.model.Dispute dispute) {
        log.info("[WORKER]: Handling dispute funds withdrawn with ID: {}", dispute.getId());
        paymentService.handleDisputeFundsWithdrawn(dispute);
      }
    }
  }
}