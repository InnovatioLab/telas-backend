package com.telas.services.impl;

import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.Subscription;
import com.telas.enums.SubscriptionStatus;
import com.telas.helpers.MonitorHelper;
import com.telas.repositories.MonitorRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.AdUnusedTrackingService;
import com.telas.services.MonitorSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonitorSubscriptionServiceImpl implements MonitorSubscriptionService {
    private final Logger log = LoggerFactory.getLogger(MonitorSubscriptionServiceImpl.class);
    private final MonitorRepository repository;
    private final SubscriptionRepository subscriptionRepository;
    private final MonitorHelper helper;

    private final AdUnusedTrackingService adUnusedTrackingService;

    @Override
    @Transactional
    public void removeMonitorAdsFromSubscription(Subscription subscription) {
        List<SubscriptionStatus> validStatuses = List.of(SubscriptionStatus.EXPIRED, SubscriptionStatus.CANCELLED);

        if (!validStatuses.contains(subscription.getStatus())) {
            return;
        }

        Client client = subscription.getClient();
        List<Monitor> updatedMonitors = new ArrayList<>();
        Set<UUID> touchedAdIds = new HashSet<>();

        subscription.getMonitors().forEach(monitor -> {
            List<String> adNamesToRemove = monitor.getAds().stream()
                    .filter(ad -> ad.getClient().getId().equals(client.getId()))
                    .map(Ad::getName)
                    .toList();

            if (!adNamesToRemove.isEmpty()) {
                monitor.getMonitorAds()
                        .stream()
                        .filter(monitorAd -> adNamesToRemove.contains(monitorAd.getAd().getName()))
                        .forEach(monitorAd -> touchedAdIds.add(monitorAd.getAd().getId()));
                monitor.getMonitorAds().removeIf(monitorAd -> adNamesToRemove.contains(monitorAd.getAd().getName()));
                updatedMonitors.add(monitor);

                if (monitor.isAbleToSendBoxRequest()) {
                    helper.sendBoxesMonitorsRemoveAds(monitor, adNamesToRemove);
                }
            }
        });

        if (!updatedMonitors.isEmpty()) {
            repository.saveAll(updatedMonitors);
        }

        subscription.getSubscriptionMonitors().clear();
        subscriptionRepository.save(subscription);
        adUnusedTrackingService.syncUnusedStateForAdIds(touchedAdIds);
    }
}
