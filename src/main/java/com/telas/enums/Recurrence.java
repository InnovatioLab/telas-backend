package com.telas.enums;

import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.shared.constants.valitation.SubscriptionValidationMessages;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Getter
public enum Recurrence {
    THIRTY_DAYS(30, BigDecimal.valueOf(1L)),
    SIXTY_DAYS(60, BigDecimal.valueOf(2L)),
    NINETY_DAYS(90, BigDecimal.valueOf(3L)),
    MONTHLY(0, BigDecimal.valueOf(1L));

    private final long days;
    private final BigDecimal multiplier;

    Recurrence(long days, BigDecimal multiplier) {
        this.days = days;
        this.multiplier = multiplier;
    }

    public Instant calculateEndsAt(Instant startedAt) {
        if (days <= 0) {
            return null;
        }
        LocalDate startDate = startedAt.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = startDate.plusDays(days);
        return endDate.atTime(startedAt.atZone(ZoneOffset.UTC).toLocalTime()).toInstant(ZoneOffset.UTC);
    }

    public Instant calculateEndsAtUpgradeRenew(Instant currentEndsAt, Recurrence previousRecurrence) {
        long daysToAdd = days - previousRecurrence.days;
        LocalDate currentDate = currentEndsAt.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate newDate = currentDate.plusDays(daysToAdd);
        return newDate.atTime(currentEndsAt.atZone(ZoneOffset.UTC).toLocalTime()).toInstant(ZoneOffset.UTC);
    }

    public void validateUpgradeTo(Recurrence target) {
        List<Recurrence> allowed = List.of(SIXTY_DAYS, NINETY_DAYS, MONTHLY);

        if (!allowed.contains(target)) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_RECURRENCE);
        }
        if (this == MONTHLY) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_MONTHLY);
        }
        if (this == target) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_TO_SAME_RECURRENCE);
        }
        if (this == SIXTY_DAYS && target != NINETY_DAYS && target != MONTHLY) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FROM_SIXTY_DAYS);
        }
        if (this == NINETY_DAYS && target != MONTHLY) {
            throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FROM_NINETY_DAYS);
        }
    }
}
