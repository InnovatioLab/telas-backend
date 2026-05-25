package com.telas.services.payment;

import com.telas.entities.*;
import com.telas.enums.NotificationReference;
import com.telas.enums.Recurrence;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.SubscriptionFlowRepository;
import com.telas.services.CartService;
import com.telas.services.EmailService;
import com.telas.services.NotificationService;
import com.telas.shared.utils.DateUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SubscriptionPurchaseCompletionHandler {

    private final Logger log = LoggerFactory.getLogger(SubscriptionPurchaseCompletionHandler.class);
    private final SubscriptionFlowRepository subscriptionFlowRepository;
    private final CartService cartService;
    private final NotificationService notificationService;
    private final ClientRepository clientRepository;
    private final EmailService emailService;

    @Value("${admin.purchase.notification.email:}")
    private String adminPurchaseNotificationEmail;

    @Transactional
    public void handleNonRecurringPayment(Subscription subscription) {
        Client client = subscription.getClient();
        inactivateCart(client);
        deleteSubscriptionFlow(client);
        sendPurchaseConfirmationEmail(subscription);
        notifyAdminsNewPurchase(subscription);
    }

    public void sendPurchaseConfirmationEmail(Subscription subscription) {
        if (shouldSkipClientPurchaseNotification(subscription)) {
            return;
        }

        Map<String, String> params = new HashMap<>(Map.of(
                "name", subscription.getClient().getBusinessName(),
                "locations", subscription.getMonitorAddressesFormated(),
                "startDate", DateUtils.formatInstantToString(subscription.getStartedAt()),
                "link", redirectUrlAfterCreatingNewSubscription()
        ));

        if (subscription.getEndsAt() != null) {
            params.put("endDate", DateUtils.formatInstantToString(subscription.getEndsAt()));
        }

        notificationService.save(NotificationReference.FIRST_SUBSCRIPTION, subscription.getClient(), params, true);
    }

    public String redirectUrlAfterCreatingNewSubscription() {
        return "/client/next-steps";
    }

    private void inactivateCart(Client client) {
        try {
            var cart = cartService.findActiveByClientIdWithItens(client.getId());
            cartService.inactivateCart(cart);
        } catch (ResourceNotFoundException ignored) {
        }
    }

    private void deleteSubscriptionFlow(Client client) {
        Optional.ofNullable(client.getSubscriptionFlow())
                .ifPresent(subscriptionFlow -> {
                    subscriptionFlowRepository.deleteById(subscriptionFlow.getId());
                    client.setSubscriptionFlow(null);
                });
    }

    private boolean shouldSkipClientPurchaseNotification(Subscription subscription) {
        if (subscription == null || subscription.isBonus()) {
            return true;
        }
        Client client = subscription.getClient();
        return client == null || client.isPartner();
    }

    private void notifyAdminsNewPurchase(Subscription subscription) {
        Map<String, String> params = buildAdminNewPurchaseParams(subscription);
        clientRepository.findAllAdmins().forEach(admin ->
                notificationService.save(NotificationReference.ADMIN_NEW_PURCHASE, admin, params, true));

        if (!ValidateDataUtils.isNullOrEmptyString(adminPurchaseNotificationEmail)) {
            try {
                var emailData = NotificationReference.ADMIN_NEW_PURCHASE.getEmailData(params);
                if (emailData != null) {
                    emailData.setEmail(adminPurchaseNotificationEmail.trim());
                    emailData.getParams().put("clientId", subscription.getClient().getId().toString());
                    emailService.send(emailData);
                }
            } catch (RuntimeException e) {
                log.error("Failed to send admin new purchase email to configured address", e);
            }
        }
    }

    private Map<String, String> buildAdminNewPurchaseParams(Subscription subscription) {
        Client client = subscription.getClient();
        Map<String, String> params = new HashMap<>();
        params.put("buyerName", client.getBusinessName() != null ? client.getBusinessName() : "");
        params.put("subscriptionId", subscription.getId().toString());
        params.put("monitorsDetailHtml", buildMonitorsDetailHtml(subscription));
        params.put("attachmentListHtml", buildAttachmentListHtml(client));
        params.put("veiculationSummary", formatVeiculationPeriod(subscription));
        return params;
    }

    private String buildMonitorsDetailHtml(Subscription subscription) {
        List<SubscriptionMonitor> ordered = subscription.getSubscriptionMonitors().stream()
                .sorted(Comparator.comparing(sm -> sm.getMonitor().getId()))
                .toList();
        StringBuilder sb = new StringBuilder();
        for (SubscriptionMonitor sm : ordered) {
            Monitor monitor = sm.getMonitor();
            Address address = monitor.getAddress();
            String label = address.getLocationName() != null && !address.getLocationName().isBlank()
                    ? HtmlUtils.htmlEscape(address.getLocationName().trim())
                    : "Display location";
            sb.append("<div style=\"margin-bottom:18px;border-bottom:1px solid #e0e0e0;padding-bottom:12px;\">");
            sb.append("<strong>").append(label).append("</strong><br/>");
            sb.append("Blocks purchased: ").append(HtmlUtils.htmlEscape(String.valueOf(sm.getSlotsQuantity()))).append("<br/>");
            sb.append(address.getFullAddressFormattedHtml());
            sb.append("</div>");
        }
        if (sb.isEmpty()) {
            return "<p>No monitors linked to this subscription.</p>";
        }
        return sb.toString();
    }

    private String buildAttachmentListHtml(Client client) {
        Optional<Ad> adOpt = resolveOldestApprovedAd(client);
        if (adOpt.isEmpty()) {
            return "<p>No approved ad linked for this order.</p>";
        }
        List<Attachment> attachments = adOpt.get().getAttachments().stream()
                .sorted(Comparator.comparing(Attachment::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (attachments.isEmpty()) {
            return "<p>No files linked to the ad for this order.</p>";
        }
        StringBuilder ul = new StringBuilder("<ul style=\"margin:0;padding-left:20px;\">");
        for (Attachment att : attachments) {
            ul.append("<li>")
                    .append(HtmlUtils.htmlEscape(att.getName()))
                    .append(" (")
                    .append(HtmlUtils.htmlEscape(att.getType()))
                    .append(")</li>");
        }
        ul.append("</ul>");
        return ul.toString();
    }

    private Optional<Ad> resolveOldestApprovedAd(Client client) {
        return client.getApprovedAds().stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparing(Ad::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private String formatVeiculationPeriod(Subscription subscription) {
        Recurrence recurrence = subscription.getRecurrence();
        String start = subscription.getStartedAt() != null
                ? DateUtils.formatInstantToString(subscription.getStartedAt())
                : "";
        if (Recurrence.MONTHLY.equals(recurrence)) {
            return "Monthly (continuous renewal). Start: " + start + ".";
        }
        String end = subscription.getEndsAt() != null ? DateUtils.formatInstantToString(subscription.getEndsAt()) : "";
        long days = recurrence.getDays();
        return "Plan " + recurrence.name().replace('_', ' ') + " (" + days + " days). " + start + " to " + end + ".";
    }
}
