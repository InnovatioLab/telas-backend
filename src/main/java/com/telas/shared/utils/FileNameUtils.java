package com.telas.shared.utils;

public final class FileNameUtils {

    private FileNameUtils() {
    }

    public static String sanitizeFileNameSegment(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace("\"", "_")
                .replace("\r", "")
                .replace("\n", "")
                .replace("/", "_")
                .replace("\\", "_")
                .trim();
    }

    public static String buildQuestionnaireExportFileName(String clientName, String adName) {
        String client = sanitizeFileNameSegment(clientName);
        if (!client.isEmpty()) {
            return truncate(client) + ".txt";
        }
        String ad = sanitizeFileNameSegment(adName);
        if (!ad.isEmpty()) {
            return truncate(ad) + ".txt";
        }
        return "questionnaire.txt";
    }

    private static String truncate(String value) {
        return value.length() > 200 ? value.substring(0, 200) : value;
    }
}
