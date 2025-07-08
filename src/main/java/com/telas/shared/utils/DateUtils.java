package com.telas.shared.utils;

import com.telas.shared.constants.SharedConstants;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateUtils {
  private static final String DATE_PATTERN = "MM/dd/yyyy";

  private DateUtils() {
  }

  public static String formatInstantToString(Instant date) {
    if (date == null) {
      return null;
    }

    return DateTimeFormatter.ofPattern(DATE_PATTERN)
            .withZone(ZoneId.of(SharedConstants.ZONE_ID))
            .format(date);
  }
}
