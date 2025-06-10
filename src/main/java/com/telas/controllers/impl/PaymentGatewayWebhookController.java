package com.telas.controllers.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.telas.services.PaymentService;
import com.telas.services.SubscriptionService;
import com.telas.shared.utils.MoneyUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class PaymentGatewayWebhookController {
  private final PaymentService paymentService;
  private final SubscriptionService subscriptionService;

  @Value("${STRIPE_WEBHOOK_SECRET}")
  private String webhookSecret;

  @PostMapping
  public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) throws StripeException, JsonProcessingException {

    if (ValidateDataUtils.isNullOrEmptyString(webhookSecret) || ValidateDataUtils.isNullOrEmptyString(sigHeader)) {
      ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook secret ou cabeçalho de assinatura não configurados");
    }

    try {
      Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

      switch (event.getType()) {
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
        default:
          break;
      }

      return ResponseEntity.ok("Evento processado com sucesso");
    } catch (SignatureVerificationException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Assinatura inválida");
    }
  }

  private void handlePaymentIntent(Event event) throws StripeException, JsonProcessingException {
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

    if (dataObjectDeserializer.getObject().isPresent()) {
      StripeObject stripeObject = dataObjectDeserializer.getObject().get();

      if (stripeObject instanceof PaymentIntent paymentIntent) {
        paymentService.updatePaymentStatus(paymentIntent);
      }
    }
  }

  private void handleInvoicePayment(Event event) throws JsonProcessingException {
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

    if (dataObjectDeserializer.getObject().isPresent()) {
      StripeObject stripeObject = dataObjectDeserializer.getObject().get();

      if (stripeObject instanceof Invoice invoice) {
        BigDecimal amountDue = invoice.getAmountDue() != null
                ? MoneyUtils.divide(BigDecimal.valueOf(invoice.getAmountDue()), BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Ignorar faturas com valor 0
        if (amountDue.compareTo(BigDecimal.ZERO) > 0) {
          paymentService.updatePaymentStatus(invoice);
        }
      }
    }
  }

  private void handleSubscriptionDeleted(Event event) throws JsonProcessingException {
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

    if (dataObjectDeserializer.getObject().isPresent()) {
      StripeObject stripeObject = dataObjectDeserializer.getObject().get();

      if (stripeObject instanceof Subscription subscription) {
        subscriptionService.cancelSubscription(subscription);
      }
    }
  }
}
