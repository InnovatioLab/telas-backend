package com.telas.shared.utils;

import com.telas.enums.BoxScriptVersionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoxScriptVersionUtilsTest {

    @Test
    void resolveStatus_match_when_equal() {
        assertThat(BoxScriptVersionUtils.resolveStatus("1.2.3", "1.2.3"))
                .isEqualTo(BoxScriptVersionStatus.MATCH);
    }

    @Test
    void resolveStatus_behind_when_lower_semver() {
        assertThat(BoxScriptVersionUtils.resolveStatus("1.2.0", "1.3.0"))
                .isEqualTo(BoxScriptVersionStatus.BEHIND);
    }

    @Test
    void resolveStatus_match_when_ahead_of_target() {
        assertThat(BoxScriptVersionUtils.resolveStatus("2.0.0", "1.0.0"))
                .isEqualTo(BoxScriptVersionStatus.MATCH);
    }

    @Test
    void resolveStatus_unknown_when_no_reported() {
        assertThat(BoxScriptVersionUtils.resolveStatus(null, "1.0.0"))
                .isEqualTo(BoxScriptVersionStatus.UNKNOWN);
    }

    @Test
    void resolveStatus_unknown_when_no_target() {
        assertThat(BoxScriptVersionUtils.resolveStatus("1.0.0", ""))
                .isEqualTo(BoxScriptVersionStatus.UNKNOWN);
    }

    @Test
    void compareSemverLoose_orders_numeric_parts() {
        assertThat(BoxScriptVersionUtils.compareSemverLoose("1.10.0", "1.9.0")).isPositive();
    }
}
