package com.telas.services;

import com.telas.dtos.response.BoxAddressResponseDto;

import java.util.List;

public interface BoxAddressService {
  List<BoxAddressResponseDto> findAll();
}

