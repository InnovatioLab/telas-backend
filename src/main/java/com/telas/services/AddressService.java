package com.telas.services;

import com.telas.dtos.request.AddressRequestDto;
import com.telas.dtos.response.AddressFromZipCodeResponseDto;
import com.telas.entities.Address;

import java.util.UUID;

public interface AddressService {
  Address save(Address address);

  Address save(AddressRequestDto request);

  AddressFromZipCodeResponseDto findByZipCode(String zipCode);

  Address findById(UUID id);

  Address getOrCreateAddress(AddressRequestDto addressRequestDto);
}

