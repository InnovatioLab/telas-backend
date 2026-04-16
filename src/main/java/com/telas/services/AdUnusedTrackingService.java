package com.telas.services;

import java.util.Collection;
import java.util.UUID;

public interface AdUnusedTrackingService {

    void syncUnusedStateForAdIds(Collection<UUID> adIds);
}
