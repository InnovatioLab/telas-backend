package com.telas.shared.constants.valitation;

import com.telas.shared.constants.SharedConstants;

public final class MonitorValidationMessages {
  public static final String SIZE_INVALID = "Size must be a positive number with up to two decimal places.";
  public static final String MONITOR_NOT_FOUND = "Monitor not found";
  public static final String LOCATION_DESCRIPTION_SIZE = "Location description must be at most 255 characters";
  public static final String CLIENT_NOT_PARTNER = "Client is not a partner";
  public static final String AD_ID_REQUIRED = "Ad ID is required";
  public static final String ORDER_REQUIRED = "Order is required";
  public static final String ORDER_INVALID = "Order must be a positive number";
  public static final String ADS_ORDER_INDEX_DUPLICATED = "Order index is duplicated";
  public static final String MAX_MONITOR_ADS = "Maximum number of ads per monitor is " + SharedConstants.MAX_MONITOR_ADS;
  public static final String ZIP_CODE_LIST_EMPTY = "Zip code list cannot be empty";
  public static final String MONITOR_INACTIVE_OR_BLOCKS_UNAVAILABLE = "Monitor is inactive or does not have enough available blocks";
  public static final String ADDRESS_REQUIRED = "Address is required";
  public static final String ADDRESS_ID_AND_ADDRESS_BOTH_PROVIDED = "Both address and address ID cannot be provided at the same time";
  public static final String AD_NOT_ABLE_TO_ASSIGN_TO_MONITOR = "Ad cannot be assigned to the monitor. It might not be approved, not associated with the monitor, or linked to an invalid subscription.";
  public static final String MONITOR_HAS_ACTIVE_SUBSCRIPTION = "Monitor has an active subscription. Please cancel the subscription before deleting the monitor.";
}
