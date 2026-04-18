package com.telas.shared.utils;

import com.telas.shared.constants.SharedConstants;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class DateUtils {
  private static final String DATE_PATTERN = "MM/dd/yyyy";
  private static final String US_DATETIME_PATTERN = "MM/dd/yyyy hh:mm:ss a z";

  private DateUtils() {
  }

  public static String formatInstantToUsDateTime(Instant instant) {
    if (instant == null) {
      return null;
    }
    return DateTimeFormatter.ofPattern(US_DATETIME_PATTERN)
            .withZone(ZoneId.of(SharedConstants.ZONE_ID))
            .format(instant);
  }

  public static String formatInstantToString(Instant date) {
    if (date == null) {
      return null;
    }

    return DateTimeFormatter.ofPattern(DATE_PATTERN)
            .withZone(ZoneId.of(SharedConstants.ZONE_ID))
            .format(date);
  }

  public static String formatDurationHuman(Duration duration) {
    if (duration == null || duration.isNegative()) {
      return "";
    }
    long totalSeconds = duration.getSeconds();
    if (totalSeconds == 0 && duration.getNano() == 0) {
      return "0 seconds";
    }
    long days = totalSeconds / 86400;
    long hours = (totalSeconds % 86400) / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    List<String> parts = new ArrayList<>();
    if (days > 0) {
      parts.add(days == 1 ? "1 day" : days + " days");
    }
    if (hours > 0) {
      parts.add(hours == 1 ? "1 hour" : hours + " hours");
    }
    if (minutes > 0) {
      parts.add(minutes == 1 ? "1 minute" : minutes + " minutes");
    }
    if (seconds > 0 || parts.isEmpty()) {
      parts.add(seconds == 1 ? "1 second" : seconds + " seconds");
    }
    return String.join(", ", parts);
  }
}
