package com.telas.services;

import com.telas.dtos.response.IncidentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IncidentQueryService {

    Page<IncidentResponseDto> findAll(Pageable pageable);
}
