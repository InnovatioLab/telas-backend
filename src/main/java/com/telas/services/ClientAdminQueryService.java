package com.telas.services;

import com.telas.dtos.request.filters.ClientFilterRequestDto;
import com.telas.dtos.request.filters.FilterAdRequestDto;
import com.telas.dtos.response.AdRequestAdminResponseDto;
import com.telas.dtos.response.ClientMinResponseDto;
import com.telas.dtos.response.PaginationResponseDto;
import com.telas.dtos.response.PendingAdAdminValidationResponseDto;

import java.util.List;

public interface ClientAdminQueryService {

    PaginationResponseDto<List<ClientMinResponseDto>> findAllFilters(ClientFilterRequestDto request);

    PaginationResponseDto<List<AdRequestAdminResponseDto>> findPendingAdRequest(FilterAdRequestDto request);

    PaginationResponseDto<List<PendingAdAdminValidationResponseDto>> findPendingAds(FilterAdRequestDto request);
}
