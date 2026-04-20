package com.telas.monitoring.plug;

import com.telas.monitoring.entities.SmartPlugEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "monitoring.kasa.mode", havingValue = "stub")
public class StubSmartPlugClient implements SmartPlugClient {

    @Override
    public PlugReading read(SmartPlugEntity plug, SmartPlugCredentials credentials) {
        return readAtHost(plug, plug.getLastSeenIp(), credentials);
    }

    @Override
    public PlugReading readAtHost(SmartPlugEntity plug, String host, SmartPlugCredentials credentials) {
        if (host == null || host.isBlank()) {
            return PlugReading.unreachable("missing_host");
        }
        String last = plug.getLastSeenIp();
        if (last != null && host.trim().equals(last.trim())) {
            return new PlugReading(true, true, 42.0, 120.0, 0.35, null);
        }
        return PlugReading.unreachable("stub_probe_miss");
    }
}
