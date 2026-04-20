package com.telas.services;

import com.telas.dtos.request.SmartPlugAccountCreateRequestDto;
import com.telas.dtos.request.SmartPlugAccountUpdateRequestDto;
import com.telas.dtos.response.SmartPlugAccountResponseDto;

import java.util.List;
import java.util.UUID;

public interface SmartPlugAccountAdminService {

    List<SmartPlugAccountResponseDto> listByBox(UUID boxId);

    SmartPlugAccountResponseDto getById(UUID id);

    SmartPlugAccountResponseDto create(SmartPlugAccountCreateRequestDto dto);

    SmartPlugAccountResponseDto update(UUID id, SmartPlugAccountUpdateRequestDto dto);

    void delete(UUID id);
}
