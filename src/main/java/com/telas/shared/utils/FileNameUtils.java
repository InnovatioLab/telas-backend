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
        String ad = sanitizeFileNameSegment(adName);
        if (client.isEmpty() && ad.isEmpty()) {
            return "questionnaire.txt";
        }
        if (client.isEmpty()) {
            return truncate(ad) + ".txt";
        }
        if (ad.isEmpty()) {
            return truncate(client) + ".txt";
        }
        return truncate(client + ad) + ".txt";
    }

    private static String truncate(String value) {
        return value.length() > 200 ? value.substring(0, 200) : value;
    }
}
