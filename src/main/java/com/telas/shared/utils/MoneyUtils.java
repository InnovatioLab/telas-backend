package com.telas.shared.utils;

import com.telas.infra.exceptions.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {
  private static final int SCALE = 2;
  private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

  private MoneyUtils() {
  }

  public static BigDecimal divide(BigDecimal value1, BigDecimal value2) {
    if (value1 == null || value2 == null) {
      throw new BusinessRuleException("The amounts can't be null");
    }
    return value1.divide(value2, SCALE, ROUNDING_MODE);
  }

  public static BigDecimal multiply(BigDecimal value1, BigDecimal value2) {
    if (value1 == null || value2 == null) {
      throw new BusinessRuleException("The amounts can't be null");
    }
    return value1.multiply(value2).setScale(SCALE, ROUNDING_MODE);
  }

  public static BigDecimal subtract(BigDecimal value1, BigDecimal value2) {
    if (value1 == null || value2 == null) {
      throw new BusinessRuleException("The amounts can't be null");
    }
    return value1.subtract(value2).setScale(SCALE, ROUNDING_MODE);
  }

  public static BigDecimal add(BigDecimal value1, BigDecimal value2) {
    if (value1 == null || value2 == null) {
      throw new BusinessRuleException("The amounts can't be null");
    }
    return value1.add(value2).setScale(SCALE, ROUNDING_MODE);
  }
}
