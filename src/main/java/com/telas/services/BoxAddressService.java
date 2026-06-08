package com.telas.services;

import com.telas.dtos.request.BoxAddressRequestDto;
import com.telas.dtos.response.BoxAddressResponseDto;

import java.util.List;
import java.util.UUID;

public interface BoxAddressService {
  List<BoxAddressResponseDto> findAll();

  List<BoxAddressResponseDto> findAllForAdmin();

  BoxAddressResponseDto create(BoxAddressRequestDto dto);

  BoxAddressResponseDto update(UUID id, BoxAddressRequestDto dto);

  void delete(UUID id);
}

