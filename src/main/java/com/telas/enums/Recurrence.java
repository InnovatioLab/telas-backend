package com.telas.enums;

import java.time.Instant;

public enum Recurrence {
    THIRTY_DAYS(30),
    SIXTY_DAYS(60),
    NINETY_DAYS(90),
    MONTHLY(0);

    private final long days;

    Recurrence(long days) {
        this.days = days;
    }

    public Instant calculateEndsAt(Instant startedAt) {
        return days > 0 ? startedAt.plusSeconds(days * 24 * 60 * 60) : null;
    }
}
