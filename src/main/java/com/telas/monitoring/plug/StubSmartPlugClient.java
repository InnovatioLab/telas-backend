package com.telas.monitoring.plug;

import com.telas.monitoring.entities.SmartPlugEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "monitoring.kasa.mode", havingValue = "stub")
public class StubSmartPlugClient implements SmartPlugClient {

    @Override
    public PlugReading read(SmartPlugEntity plug, SmartPlugCredentials credentials) {
        return new PlugReading(true, true, 42.0, 120.0, 0.35, null);
    }
}
