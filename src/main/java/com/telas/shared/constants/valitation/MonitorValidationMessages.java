package com.telas.shared.constants.valitation;

public final class MonitorValidationMessages {
  public static final String SIZE_INVALID = "Size must be a positive number with up to two decimal places.";
  public static final String MONITOR_NOT_FOUND = "Monitor not found";
  public static final String MAX_BLOCKS_INVALID = "Max blocks must be a positive number";
  public static final String LOCATION_DESCRIPTION_SIZE = "Location description must be at most 255 characters";
  public static final String CLIENT_NOT_PARTNER = "Client is not a partner";
  public static final String AD_ID_REQUIRED = "Ad ID is required";
  public static final String BLOCK_TIME_REQUIRED = "Block time is required";
  public static final String BLOCK_TIME_INVALID = "Block time must be a positive number";
  public static final String ORDER_REQUIRED = "Order is required";
  public static final String ORDER_INVALID = "Order must be a positive number";
  public static final String ADS_ORDER_INDEX_DUPLICATED = "Order index is duplicated";
  public static final String MAX_MONITOR_ADS = "Maximum number of ads per monitor is 12";
  public static final String SUM_BLOCK_TIME_INVALID = "Sum of block time exceeds the maximum allowed time";
  public static final String ADDRESS_ID_REQUIRED = "Address ID is required";
  public static final String BLOCK_TIME_MAX_VALUE = "Block time must be less than or equal to 60 seconds";
  public static final String ZIP_CODE_LIST_EMPTY = "Zip code list cannot be empty";
  public static final String MONITOR_INACTIVE = "Monitor is inactive";
  public static final String MONITOR_BLOCKS_UNAVAILABLE = "Monitor does not have enough available blocks";
  public static final String MONITOR_INACTIVE_OR_BLOCKS_UNAVAILABLE = "Monitor is inactive or does not have enough available blocks";
  public static final String BLOCK_PRICE_INVALID = "Block price must be a positive number with up to two decimal places";
}
