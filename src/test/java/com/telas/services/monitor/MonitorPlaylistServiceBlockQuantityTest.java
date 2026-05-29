package com.telas.services.monitor;

import com.telas.entities.Ad;
import com.telas.shared.constants.SharedConstants;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonitorPlaylistServiceBlockQuantityTest {

    private final MonitorPlaylistService service =
            new MonitorPlaylistService(null, null, null, null, null, null);

    @Test
    void distributePartnerBlockQuantitiesAlwaysUsesOneBlockPerAd() {
        Ad first = new Ad();
        first.setId(UUID.randomUUID());
        Ad second = new Ad();
        second.setId(UUID.randomUUID());

        Map<UUID, Integer> quantities = service.distributePartnerBlockQuantities(List.of(first, second));

        assertEquals(SharedConstants.MIN_QUANTITY_MONITOR_BLOCK, quantities.get(first.getId()));
        assertEquals(SharedConstants.MIN_QUANTITY_MONITOR_BLOCK, quantities.get(second.getId()));
    }
}
