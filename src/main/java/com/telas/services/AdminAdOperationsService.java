package com.telas.services;

import com.telas.dtos.request.filters.AdminAdOperationsFilterRequestDto;
import com.telas.dtos.response.AdminAdOperationRowDto;
import com.telas.dtos.response.AdminExpiryNotificationDto;
import com.telas.dtos.response.PaginationResponseDto;

import java.util.List;
import java.util.UUID;

public interface AdminAdOperationsService {

    PaginationResponseDto<List<AdminAdOperationRowDto>> findPage(AdminAdOperationsFilterRequestDto request);

    List<AdminExpiryNotificationDto> listExpiryNotifications(UUID advertiserClientId);

    byte[] exportSubscriptionsCsv();
}
