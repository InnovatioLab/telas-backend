package com.telas.controllers.impl;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.telas.services.PaymentService;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class PaymentGatewayWebhookController {
  private final PaymentService paymentService;

  @Value("${STRIPE_WEBHOOK_SECRET}")
  private String webhookSecret;

  @Value("${PAYMENT_GATEWAY_API_KEY}")
  private String key;

  @PostMapping
  public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {

    if (ValidateDataUtils.isNullOrEmptyString(webhookSecret) || ValidateDataUtils.isNullOrEmptyString(sigHeader)) {
      ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook secret ou cabeçalho de assinatura não configurados");
    }

    Stripe.apiKey = key;

    try {
      // Verificar a assinatura do webhook
      Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

      switch (event.getType()) {
        case "payment_intent.succeeded":
        case "payment_intent.canceled":
        case "payment_intent.payment_failed":
          handlePaymentIntent(event);
          break;
//        case "customer.subscription.updated":
        case "customer.subscription.deleted":
          handleSubscriptionEvent(event);
          break;
        case "invoice.payment_failed":
        case "invoice.payment_succeeded":
          handleInvoicePayment(event);
          break;
        default:
          break;
      }

      return ResponseEntity.ok("Evento processado com sucesso");
    } catch (SignatureVerificationException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Assinatura inválida");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao processar o evento");
    }
  }

  private void handlePaymentIntent(Event event) {
    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);

    if (paymentIntent != null) {
      paymentService.updatePaymentStatus(paymentIntent);
    }
  }

  private void handleInvoicePayment(Event event) {
    Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
    if (invoice != null) {
      paymentService.updatePaymentStatus(invoice);
    }
  }

  private void handleSubscriptionEvent(Event event) {
    Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);

    if (subscription != null) {
//      paymentService.updatePaymentStatus(subscription);
    }
  }
}
