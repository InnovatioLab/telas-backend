package com.telas.services;

import com.telas.dtos.response.AddressFromZipCodeResponseDto;
import com.telas.entities.Address;

import java.util.UUID;

public interface AddressService {
    Address save(Address address);

    AddressFromZipCodeResponseDto findByZipCode(String zipCode);

    Address findById(UUID id);

    Address findAddressPartnerById(UUID id);
}

