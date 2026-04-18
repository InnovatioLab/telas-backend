package com.telas.shared.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {

    @Test
    void formatDurationHuman_zero() {
        assertThat(DateUtils.formatDurationHuman(Duration.ZERO)).isEqualTo("0 seconds");
    }

    @Test
    void formatDurationHuman_secondsOnly() {
        assertThat(DateUtils.formatDurationHuman(Duration.ofSeconds(45))).isEqualTo("45 seconds");
    }

    @Test
    void formatDurationHuman_oneSecond() {
        assertThat(DateUtils.formatDurationHuman(Duration.ofSeconds(1))).isEqualTo("1 second");
    }

    @Test
    void formatDurationHuman_minutesAndSeconds() {
        assertThat(DateUtils.formatDurationHuman(Duration.ofMinutes(2).plusSeconds(3)))
                .isEqualTo("2 minutes, 3 seconds");
    }

    @Test
    void formatDurationHuman_hoursMixed() {
        assertThat(DateUtils.formatDurationHuman(Duration.ofHours(3).plusMinutes(1).plusSeconds(1)))
                .isEqualTo("3 hours, 1 minute, 1 second");
    }

    @Test
    void formatDurationHuman_days() {
        assertThat(DateUtils.formatDurationHuman(Duration.ofDays(1).plusHours(2)))
                .isEqualTo("1 day, 2 hours");
    }

    @Test
    void formatDurationHuman_nullOrNegative_returnsEmpty() {
        assertThat(DateUtils.formatDurationHuman(null)).isEmpty();
        assertThat(DateUtils.formatDurationHuman(Duration.ofSeconds(-1))).isEmpty();
    }
}
