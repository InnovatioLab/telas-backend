package com.telas.services.impl;

import com.telas.dtos.response.IncidentResponseDto;
import com.telas.monitoring.repositories.IncidentEntityRepository;
import com.telas.services.IncidentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncidentQueryServiceImpl implements IncidentQueryService {

    private final IncidentEntityRepository incidentEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponseDto> findAll(Pageable pageable) {
        return incidentEntityRepository.findAllByOrderByOpenedAtDesc(pageable).map(IncidentResponseDto::new);
    }
}
