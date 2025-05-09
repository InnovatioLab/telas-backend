package com.telas.services.impl;

import com.telas.dtos.response.AddressFromZipCodeResponseDto;
import com.telas.entities.Address;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.AddressRepository;
import com.telas.services.AddressService;
import com.telas.shared.constants.valitation.AddressValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final AddressRepository repository;

    @Override
    @Transactional
    public Address save(Address address) {
        return repository.save(address);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressFromZipCodeResponseDto findByZipCode(String zipCode) {
        List<Address> addresses = repository.findByZipCode(zipCode);

        return addresses.stream()
                .filter(Address::hasLocation)
                .findFirst()
                .map(AddressFromZipCodeResponseDto::new)
                .orElseGet(() -> addresses.isEmpty() ? null : new AddressFromZipCodeResponseDto(addresses.get(0)));
    }

    @Override
    @Transactional
    public Address findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AddressValidationMessages.ADDRESS_NOT_FOUND));
    }

    @Override
    @Transactional
    public Address findAddressPartnerById(UUID id) {
        return repository.findAddressPartnerById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AddressValidationMessages.ADDRESS_NOT_FOUND));
    }
}
