package com.telas.helpers;

import com.telas.entities.Subscription;
import com.telas.enums.NotificationReference;
import com.telas.services.NotificationService;
import com.telas.shared.utils.DateUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Component
public class SubscriptionExpiryNotificationHelper {

    private final NotificationService notificationService;

    public SubscriptionExpiryNotificationHelper(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Transactional
    public void sendAboutToExpiryEmail(Subscription subscription) {
        Map<String, String> params = new HashMap<>(Map.of(
                "name", subscription.getClient().getBusinessName(),
                "link", buildRedirectUrl("subscriptions/" + subscription.getId()),
                "endDate", DateUtils.formatInstantToString(subscription.getEndsAt())
        ));
        notificationService.save(NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_REMINDER, subscription.getClient(), params, true);
    }

    @Transactional
    public void sendTenDaysBeforeExpiryEmail(Subscription subscription) {
        notificationService.save(
                NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_10_DAYS,
                subscription.getClient(),
                buildCountdownExpiryParams(subscription, "10"),
                true
        );
    }

    @Transactional
    public void sendFiveDaysBeforeExpiryEmail(Subscription subscription) {
        notificationService.save(
                NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_5_DAYS,
                subscription.getClient(),
                buildCountdownExpiryParams(subscription, "5"),
                true
        );
    }

    @Transactional
    public void sendThreeDaysBeforeExpiryEmail(Subscription subscription) {
        notificationService.save(
                NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_3_DAYS,
                subscription.getClient(),
                buildCountdownExpiryParams(subscription, "3"),
                true
        );
    }

    @Transactional
    public void sendPenultimateDayEmail(Subscription subscription) {
        Map<String, String> params = new HashMap<>(Map.of(
                "name", subscription.getClient().getBusinessName(),
                "link", buildRedirectUrl("subscriptions/" + subscription.getId()),
                "endDate", DateUtils.formatInstantToString(subscription.getEndsAt())
        ));
        notificationService.save(NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_PENULTIMATE_DAY, subscription.getClient(), params, true);
    }

    private Map<String, String> buildCountdownExpiryParams(Subscription subscription, String daysRemaining) {
        return new HashMap<>(Map.of(
                "name", subscription.getClient().getBusinessName(),
                "link", buildRedirectUrl("subscriptions/" + subscription.getId()),
                "endDate", DateUtils.formatInstantToString(subscription.getEndsAt()),
                "daysRemaining", daysRemaining
        ));
    }

    private String buildRedirectUrl(String path) {
        return "/client/" + path;
    }
}
