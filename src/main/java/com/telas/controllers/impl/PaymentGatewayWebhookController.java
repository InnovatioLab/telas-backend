package com.telas.controllers.impl;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
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

  @PostMapping
  public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {

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
//        customer.subscription.deleted
//        invoice.marked_uncollectible
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

  private void handlePaymentIntent(Event event) throws StripeException {
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

    if (dataObjectDeserializer.getObject().isPresent()) {
      StripeObject stripeObject = dataObjectDeserializer.getObject().get();

      if (stripeObject instanceof PaymentIntent paymentIntent) {
        paymentService.updatePaymentStatus(paymentIntent);
      }
    }
  }

  private void handleInvoicePayment(Event event) {
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

    if (dataObjectDeserializer.getObject().isPresent()) {
      StripeObject stripeObject = dataObjectDeserializer.getObject().get();

      if (stripeObject instanceof Invoice invoice) {
        paymentService.updatePaymentStatus(invoice);
      }
    }
  }
}
