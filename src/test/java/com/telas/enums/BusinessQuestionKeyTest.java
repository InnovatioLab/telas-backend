package com.telas.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessQuestionKeyTest {

    @Test
    void orderedForQuestionnaire_hasTenKeys() {
        assertThat(BusinessQuestionKey.orderedForQuestionnaire()).hasSize(10);
    }

    @Test
    void isLegacyKey_detectsLegacyKeys() {
        assertThat(BusinessQuestionKey.isLegacyKey("LEGACY_SLOGAN")).isTrue();
        assertThat(BusinessQuestionKey.isLegacyKey("PRODUCT_OR_SERVICE")).isFalse();
    }
}
