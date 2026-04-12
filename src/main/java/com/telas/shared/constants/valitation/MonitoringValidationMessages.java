package com.telas.shared.constants.valitation;

public final class MonitoringValidationMessages {

    private MonitoringValidationMessages() {
    }

    public static final String INCIDENT_NOT_FOUND = "Incident not found";

    public static final String INCIDENT_ALREADY_RESOLVED_CANNOT_ACKNOWLEDGE =
            "Cannot acknowledge a resolved incident.";

    public static final String SMART_PLUG_MAC_DUPLICATE = "Smart plug MAC address is already registered.";

    public static final String SMART_PLUG_MONITOR_ALREADY_LINKED = "This monitor already has a smart plug.";

    public static final String SMART_PLUG_ENCRYPTION_REQUIRED =
            "Configure monitoring.kasa.encryption-key before storing plug passwords.";

    public static final String SMART_PLUG_NOT_FOUND = "Smart plug not found.";
}
