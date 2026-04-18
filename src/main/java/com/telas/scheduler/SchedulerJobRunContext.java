package com.telas.scheduler;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class SchedulerJobRunContext {

    private final ThreadLocal<UUID> runId = new ThreadLocal<>();
    private final ThreadLocal<Map<String, Object>> accumulator = ThreadLocal.withInitial(LinkedHashMap::new);

    public void begin(UUID id) {
        runId.set(id);
        accumulator.get().clear();
    }

    public void put(String key, Object value) {
        accumulator.get().put(key, value);
    }

    public void putAll(Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        accumulator.get().putAll(values);
    }

    public Map<String, Object> takeSummary() {
        try {
            return Map.copyOf(accumulator.get());
        } finally {
            accumulator.remove();
            runId.remove();
        }
    }
}
