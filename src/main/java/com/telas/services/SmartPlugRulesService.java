package com.telas.services;

import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.PlugReading;

public interface SmartPlugRulesService {
    void evaluate(SmartPlugEntity plug, PlugReading reading, boolean heartbeatStale);
}

