package com.telas.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

final class SchedulerJobResultSummarySupport {
    static final int MAX_SERIALIZED_BYTES = 65536;

    private SchedulerJobResultSummarySupport() {}

    static Map<String, Object> normalize(ObjectMapper objectMapper, Map<String, Object> summary) {
        if (summary == null || summary.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(summary);
            if (bytes.length <= MAX_SERIALIZED_BYTES) {
                return summary;
            }
            Map<String, Object> truncated = new HashMap<>();
            truncated.put("truncated", true);
            truncated.put("originalSerializedBytes", bytes.length);
            return truncated;
        } catch (JsonProcessingException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("serializationError", true);
            err.put("message", "unsupported_summary_payload");
            return err;
        }
    }
}
