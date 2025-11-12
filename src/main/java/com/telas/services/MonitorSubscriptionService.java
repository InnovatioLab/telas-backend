package com.telas.services;

import com.telas.entities.Subscription;
import org.springframework.transaction.annotation.Transactional;

public interface MonitorSubscriptionService {
    @Transactional
    void removeMonitorAdsFromSubscription(Subscription subscription);
}
