package com.telas.monitoring.plug;

import com.telas.monitoring.entities.SmartPlugEntity;

public interface SmartPlugClient {

    PlugReading read(SmartPlugEntity plug, String decryptedPassword);
}
