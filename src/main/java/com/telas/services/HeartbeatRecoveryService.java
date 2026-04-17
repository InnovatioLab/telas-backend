package com.telas.services;

import com.telas.entities.Box;

public interface HeartbeatRecoveryService {

    void recoverAfterSuccessfulHeartbeat(Box box);
}
