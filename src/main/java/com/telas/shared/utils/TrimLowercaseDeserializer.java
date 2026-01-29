package com.telas.shared.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Locale;

public class TrimLowercaseDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();

        if (ValidateDataUtils.isNullOrEmptyString(value)) {
            return null;
        }

        return value.trim().replaceAll("\\s{2,}", " ").toLowerCase(Locale.ROOT);
    }
}
