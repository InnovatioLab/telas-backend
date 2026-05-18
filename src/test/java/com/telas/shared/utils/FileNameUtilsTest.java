package com.telas.shared.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileNameUtilsTest {

    @Test
    void buildQuestionnaireExportFileName_concatenatesClientAndAd() {
        assertEquals("Acme CorpSummer Sale.txt",
                FileNameUtils.buildQuestionnaireExportFileName("Acme Corp", "Summer Sale"));
    }

    @Test
    void buildQuestionnaireExportFileName_sanitizesUnsafeCharacters() {
        assertEquals("Client_AAdB.txt",
                FileNameUtils.buildQuestionnaireExportFileName("Client\"A", "Ad\nB"));
    }

    @Test
    void buildQuestionnaireExportFileName_fallbackWhenEmpty() {
        assertEquals("questionnaire.txt",
                FileNameUtils.buildQuestionnaireExportFileName("", ""));
    }
}
