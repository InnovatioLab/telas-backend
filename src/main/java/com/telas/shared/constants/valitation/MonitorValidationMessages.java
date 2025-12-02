package com.telas.shared.constants.valitation;

public final class MonitorValidationMessages {
    public static final String MONITOR_NOT_FOUND = "Monitor not found";
    public static final String CLIENT_NOT_PARTNER = "Client is not a partner";
    public static final String AD_ID_REQUIRED = "Ad ID is required";
    public static final String ORDER_REQUIRED = "Order is required";
    public static final String ORDER_INVALID = "Order must be a positive number";
    public static final String ADS_ORDER_INDEX_DUPLICATED = "Order index is duplicated";
    public static final String ADDRESS_REQUIRED = "Address is required";
    public static final String ADDRESS_ID_AND_ADDRESS_BOTH_PROVIDED = "Both address and address ID cannot be provided at the same time";
    public static final String AD_NOT_ABLE_TO_ASSIGN_TO_MONITOR = "Ad cannot be assigned to the monitor. It might not be approved, not associated with the monitor, or linked to an invalid subscription.";
    public static final String MONITOR_HAS_ACTIVE_SUBSCRIPTION = "Monitor has an active subscription. Please cancel the subscription before deleting the monitor.";
    public static final String MONITOR_BLOCKS_UNAVAILABLE =
            "Monitor not have enough available blocks.";
    public static final String MONITOR_INACTIVE =
            "Monitor is inactive.";
    public static final String MONITOR_BOX_NOT_VALID =
            "Monitor is not attached to a valid box. Please ensure the monitor is linked to a box with an active state.";
    public static final String ADS_LIMIT_EXCEEDED =
            "The number of ads exceeds the maximum limit for this monitor. The maximum number of ads is: ";
    public static final String ADDRESS_ALREADY_IN_USE = "Address is already in use by another monitor.";
    public static final String BLOCK_QUANTITY_REQUIRED = "Block quantity is required";
    public static final String BLOCK_QUANTITY_POSITIVE = "Block quantity must be a positive number";
    public static final String BLOCK_QUANTITY_INVALID = "Block quantity is invalid, It must be 1, 2, or 7 (partner slots)";
    public static final String MONITOR_BLOCKS_BEYOND_LIMIT = "Total block quantity exceeds the maximum allowed for the monitor.";
    public static final String BLOCK_QUANTITY_PARTNER_DUPLICATED = "Only one ad with partner reserved slots (7) is allowed per monitor.";
    public static final String BLOCK_QUANTITY_MAXIMUM = "Block quantity exceeds the maximum of 7 allowed per ad.";
}
