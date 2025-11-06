package com.telas.services.impl;

import com.telas.dtos.request.AddressRequestDto;
import com.telas.dtos.response.AddressFromZipCodeResponseDto;
import com.telas.dtos.response.NearbySearchResponse;
import com.telas.entities.Address;
import com.telas.entities.Client;
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
    @Transactional
    public Address save(AddressRequestDto request) {
        Address address = new Address(request);
        repository.save(address);
        return address;
    }

    @Override
    @Transactional
    public void saveAddressUpdates(Address address, Double latitude, Double longitude, NearbySearchResponse.Place place, String photoUrl) {
        address.setLocation(latitude, longitude);

        if (place != null) {
            address.setLocationName(place.getDisplayName().getText());
            address.setLocationDescription(place.getEditorialSummary() != null ? place.getEditorialSummary().getText() : null);
        }

        if (photoUrl != null) {
            address.setPhotoUrl(photoUrl);
        }

        repository.save(address);
    }

    @Transactional
    @Override
    public void deleteMany(List<Address> addresses) {
        repository.deleteAll(addresses);
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
    @Transactional(readOnly = true)
    public Address findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AddressValidationMessages.ADDRESS_NOT_FOUND));
    }

    @Override
    @Transactional
    public Address getOrCreateAddress(AddressRequestDto addressRequestDto) {
        return repository.findByStreetAndCityAndStateAndZipCode(
                addressRequestDto.getStreet().toLowerCase(),
                addressRequestDto.getCity().toLowerCase(),
                addressRequestDto.getState().toLowerCase(),
                addressRequestDto.getZipCode().toLowerCase()
        ).orElseGet(() -> save(addressRequestDto));
    }

    @Override
    @Transactional
    public Address getOrCreateAddress(AddressRequestDto addressRequestDto, Client client) {
        return repository.findByStreetAndCityAndStateAndZipCodeAndClientId(
                addressRequestDto.getStreet().toLowerCase(),
                addressRequestDto.getCity().toLowerCase(),
                addressRequestDto.getState().toLowerCase(),
                addressRequestDto.getZipCode().toLowerCase(),
                client.getId()
        ).orElseGet(() -> createAddressForClient(addressRequestDto, client));
    }

    private Address createAddressForClient(AddressRequestDto addressRequestDto, Client client) {
        Address newAddress = new Address(addressRequestDto, client);
        client.getAddresses().add(newAddress);
        return newAddress;
    }
}
