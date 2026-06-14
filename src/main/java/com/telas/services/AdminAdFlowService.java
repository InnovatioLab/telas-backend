package com.telas.services;

import com.telas.dtos.response.AdFlowSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminAdFlowService {
    Page<AdFlowSummaryDto> listFlows(UUID clientId, Pageable pageable);
}
