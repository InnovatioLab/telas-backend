package com.telas.shared.constants.valitation;

public final class SubscriptionValidationMessages {
    public static final String SUBSCRIPTION_NOT_FOUND = "Subscription not found!";
    public static final String SUBSCRIPTION_NOT_FOUND_IN_STRIPE = "Subscription not found in Stripe with id: ";
    public static final String SUBSCRIPTION_WITHOUT_STRIPE_ID = "Subscription without Stripe ID!";
    public static final String SUBSCRIPTION_NOT_ACTIVE_IN_STRIPE = "Subscription not active in Stripe with id: ";
    public static final String RETRIEVE_STRIPE_SUBSCRIPTION_ERROR = "Error retrieving subscription from Stripe with subscriptionId: ";
    public static final String CLIENT_ALREADY_HAS_ACTIVE_SUBSCRIPTION_WITH_MONITOR = "Client already has an active subscription with the provided monitors.";
    public static final String SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_BONUS = "Subscription upgrade not allowed for bonus subscriptions.";
    public static final String SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_MONTHLY = "Subscription upgrade not allowed for monthly subscriptions.";
    public static final String SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_NON_ACTIVE_OR_EXPIRED = "Subscription upgrade not allowed for non-active or expired subscriptions.";
    public static final String SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_SHORT_BILLING_CYCLE = "Subscription upgrade not allowed for subscriptions with a short billing cycle. The billing cycle must be at least 30 days.";
    public static final String SUBSCRIPTION_CANCELLATION_ERROR_DURING_DISPUTE = "Subscription cancellation error during dispute, subscription id: ";
    public static final String SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_RECURRENCE = "Subscription upgrade not allowed with a recurrence of 30 days.";
    public static final String SUBSCRIPTION_UPGRADE_NOT_ALLOWED_TO_SAME_RECURRENCE = "Subscription upgrade not allowed to the same recurrence.";
    public static final String SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FROM_SIXTY_DAYS = "Subscription upgrade not allowed from a 60-day subscription, it can only be upgraded to a 90-day subscription or monthly.";
    public static final String SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FROM_NINETY_DAYS = "Subscription upgrade not allowed from a 90-day subscription, it can only be upgraded to a monthly subscription.";
    public static final String SUBSCRIPTION_ALREADY_ON_UPGRADE = "Subscription already on upgrade mode, if you miss the checkout session, please wait about 30 minutes and try again.";
    public static final String SUBSCRIPTION_RENEW_NOT_ALLOWED_FOR_BONUS = "Subscription renew not allowed for bonus subscriptions.";
    public static final String SUBSCRIPTION_RENEW_NOT_ALLOWED_FOR_MONTHLY = "Subscription renew not allowed for monthly subscriptions.";
    public static final String CLIENT_WITHOUT_STRIPE_ID = "Client is not able to access Customer Portal!";
    public static final String CLIENT_WITHOUT_SUBSCRIPTIONS = "Client don't have any subscriptions!";
}
