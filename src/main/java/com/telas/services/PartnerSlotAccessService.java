package com.telas.services;

import com.telas.entities.Client;
import com.telas.entities.Monitor;

public interface PartnerSlotAccessService {

    boolean hasGlobalSlotsPermission(Client client);

    boolean usesPartnerQuotaOnMonitor(Client client, Monitor monitor);

    int maxBlocksForClientOnMonitor(Client client, Monitor monitor);

    int usedBlocksByClientOnMonitor(Client client, Monitor monitor);

    boolean canAddBlocks(Client client, Monitor monitor, int additionalBlocks);

    int resolveCartBlockQuantity(Client client, Monitor monitor, Integer requestedQuantity);
}
