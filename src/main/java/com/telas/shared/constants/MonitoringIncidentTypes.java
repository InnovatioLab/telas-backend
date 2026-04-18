package com.telas.shared.constants;

import java.util.List;

public final class MonitoringIncidentTypes {

    public static final String HEARTBEAT_STALE = "HEARTBEAT_STALE";
    public static final String HEARTBEAT_NEVER_SEEN = "HEARTBEAT_NEVER_SEEN";
    public static final String CONNECTIVITY_PROBE_FAILED = "CONNECTIVITY_PROBE_FAILED";

    public static final List<String> BOX_OUTAGE_INCIDENT_TYPES =
            List.of(HEARTBEAT_STALE, HEARTBEAT_NEVER_SEEN, CONNECTIVITY_PROBE_FAILED);

    private MonitoringIncidentTypes() {}
}
