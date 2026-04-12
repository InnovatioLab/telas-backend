package com.telas.services;

import com.telas.dtos.request.SmartPlugRequestDto;
import com.telas.dtos.response.SmartPlugReadingResponseDto;
import com.telas.dtos.response.SmartPlugResponseDto;

import java.util.List;
import java.util.UUID;

public interface SmartPlugAdminService {

    List<SmartPlugResponseDto> findAll();

    SmartPlugResponseDto create(SmartPlugRequestDto dto);

    SmartPlugResponseDto update(UUID id, SmartPlugRequestDto dto);

    void delete(UUID id);

    SmartPlugReadingResponseDto testRead(UUID id);
}
