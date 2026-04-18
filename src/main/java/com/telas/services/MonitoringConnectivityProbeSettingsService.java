package com.telas.services;

public interface MonitoringConnectivityProbeSettingsService {

    long getIntervalMs();

    long setIntervalMs(long intervalMs);
}
