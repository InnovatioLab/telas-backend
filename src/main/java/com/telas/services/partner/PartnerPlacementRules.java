package com.telas.services.partner;

import com.telas.entities.Client;
import com.telas.entities.Monitor;
import org.springframework.stereotype.Service;

@Service
public class PartnerPlacementRules {

    public boolean partnerOwnsMonitorAddress(Client partner, Monitor monitor) {
        if (partner == null || monitor == null || monitor.getAddress() == null
                || monitor.getAddress().getClient() == null) {
            return false;
        }
        return monitor.getAddress().getClient().getId().equals(partner.getId());
    }

    public boolean isForeignPlacementForPartner(Client partner, Monitor monitor) {
        if (monitor == null || monitor.getAddress() == null || monitor.getAddress().getClient() == null) {
            return false;
        }
        return !partnerOwnsMonitorAddress(partner, monitor);
    }

    public boolean partnerOwnsMonitor(Monitor monitor, Client client) {
        return monitor != null && client != null && monitor.isPartner(client);
    }
}
