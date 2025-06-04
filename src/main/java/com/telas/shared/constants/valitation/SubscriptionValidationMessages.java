package com.telas.shared.constants.valitation;

public final class SubscriptionValidationMessages {
  public static final String SUBSCRIPTION_NOT_FOUND = "Subscription not found!";
  public static final String SUBSCRIPTION_NOT_FOUND_IN_STRIPE = "Subscription not found in Stripe with id: ";
  public static final String SUBSCRIPTION_WITHOUT_STRIPE_ID = "Subscription without Stripe ID!";
  public static final String SUBSCRIPTION_NOT_ACTIVE_IN_STRIPE = "Subscription not active in Stripe with id: ";
  public static final String RETRIEVE_STRIPE_SUBSCRIPTION_ERROR = "Error retrieving subscription from Stripe with subscriptionId: ";
  public static final String CLIENT_ALREADY_HAS_ACTIVE_SUBSCRIPTION_WITH_MONITOR = "Client already has an active subscription with the provided monitors.";
}
