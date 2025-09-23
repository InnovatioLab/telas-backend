package com.telas.services;

import com.telas.dtos.request.AddressRequestDto;
import com.telas.dtos.response.AddressFromZipCodeResponseDto;
import com.telas.entities.Address;
import com.telas.entities.Client;

import java.util.List;
import java.util.UUID;

public interface AddressService {
    Address save(Address address);

    Address save(AddressRequestDto request);

    void deleteMany(List<Address> addresses);

    AddressFromZipCodeResponseDto findByZipCode(String zipCode);

    Address findById(UUID id);

    Address getOrCreateAddress(AddressRequestDto addressRequestDto);

    Address getOrCreateAddress(AddressRequestDto addressRequestDto, Client client);
}

