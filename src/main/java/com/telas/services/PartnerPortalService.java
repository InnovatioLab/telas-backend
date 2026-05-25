package com.telas.services;

import com.telas.dtos.request.CreatePartnerRequestDto;
import com.telas.dtos.request.PartnerAdRemovalRequestDto;
import com.telas.dtos.response.ClientMinResponseDto;
import com.telas.dtos.response.PendingAdAdminValidationResponseDto;
import com.telas.dtos.response.WishlistResponseDto;

import java.util.List;
import java.util.UUID;

public interface PartnerPortalService {

    void changeRoleToPartner(UUID clientId);

    ClientMinResponseDto createPartnerByAdmin(CreatePartnerRequestDto request);

    List<PendingAdAdminValidationResponseDto> findMyPendingValidationAds();

    void requestPartnerAdRemoval(UUID adId, PartnerAdRemovalRequestDto request);

    void addMonitorToWishlist(UUID monitorId);

    WishlistResponseDto getWishlistMonitors();
}
