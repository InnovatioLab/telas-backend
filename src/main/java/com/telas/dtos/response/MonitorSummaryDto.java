package com.telas.dtos.response;

import java.util.UUID;

public record MonitorSummaryDto(
        UUID monitorId,
        String productId,
        String partnerName,
        String street,
        String city,
        Double latitude,
        Double longitude,
        int activeAds
) {}
