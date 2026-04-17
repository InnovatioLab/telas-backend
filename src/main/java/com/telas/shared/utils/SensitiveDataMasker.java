package com.telas.shared.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Locale;
import java.util.Set;

public final class SensitiveDataMasker {

    private static final String MASK = "***";

    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password",
        "pass",
        "passwd",
        "token",
        "access_token",
        "refresh_token",
        "authorization",
        "api_key",
        "apikey",
        "secret",
        "client_secret",
        "cookie",
        "set-cookie"
    );

    private SensitiveDataMasker() {
    }

    public static JsonNode maskJson(ObjectMapper mapper, JsonNode input) {
        if (input == null) {
            return null;
        }
        JsonNode copy = input.deepCopy();
        maskInPlace(mapper, copy);
        return copy;
    }

    private static void maskInPlace(ObjectMapper mapper, JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.fieldNames().forEachRemaining(field -> {
                JsonNode value = obj.get(field);
                if (isSensitiveKey(field)) {
                    obj.set(field, TextNode.valueOf(MASK));
                } else {
                    maskInPlace(mapper, value);
                }
            });
            return;
        }
        if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                maskInPlace(mapper, arr.get(i));
            }
        }
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(normalized);
    }
}

