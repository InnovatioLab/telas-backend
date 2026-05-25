package com.telas.services;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.filters.AdminAdOperationsFilterRequestDto;
import com.telas.dtos.response.AdPreviewLinkResponseDto;
import com.telas.dtos.response.AdminAdOperationRowDto;
import com.telas.dtos.response.AdminExpiryNotificationDto;
import com.telas.dtos.response.PaginationResponseDto;

import java.util.List;
import java.util.UUID;

public interface AdminAdOperationsService {

    PaginationResponseDto<List<AdminAdOperationRowDto>> findPage(AdminAdOperationsFilterRequestDto request);

    AdPreviewLinkResponseDto getAdPreviewLink(UUID adId);

    List<AdminExpiryNotificationDto> listExpiryNotifications(UUID advertiserClientId);

    byte[] exportSubscriptionsCsv();

    void deleteApprovedAd(UUID adId);

    void dispatchAdToBox(UUID adId);

    void addAdToPlaylist(UUID adId);

    void deliverPartnerCreativeForReview(UUID adId, AttachmentRequestDto request);
}
