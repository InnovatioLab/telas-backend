package com.telas.services;

import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.SmartPlugCredentials;

public interface SmartPlugCredentialsResolver {

    SmartPlugCredentials resolve(SmartPlugEntity plug);
}
