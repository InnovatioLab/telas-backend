package com.telas.services.client;

import com.telas.dtos.request.AddressRequestDto;
import com.telas.entities.Address;
import com.telas.entities.Client;
import com.telas.enums.Role;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.repositories.MonitorRepository;
import com.telas.services.AddressService;
import com.telas.services.MapsService;
import com.telas.shared.constants.valitation.AddressValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientAddressService {

    private final AddressService addressService;
    private final MapsService mapsService;
    private final MonitorRepository monitorRepository;

    @Transactional
    public void verifyAddressesUnique(List<AddressRequestDto> addresses, Client client) {
        if (!ValidateDataUtils.isNullOrEmpty(addresses) && Objects.nonNull(client)) {
            addressService.createEntityList(addresses, client);
        }
    }

    @Transactional
    public void updateAddresses(List<AddressRequestDto> requestList, Client client) {
        Set<UUID> receivedAddressIds = requestList.stream()
                .map(AddressRequestDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Address> addressesToRemove = client.getAddresses().stream()
                .filter(address -> !receivedAddressIds.contains(address.getId()))
                .toList();

        addressesToRemove.forEach(address -> {
            if (monitorRepository.existsByAddressId(address.getId())) {
                throw new BusinessRuleException(AddressValidationMessages.ADDRESS_IN_USE_BY_MONITOR);
            }
        });

        requestList.forEach(addressRequest -> {
            if (addressRequest.getId() == null) {
                createAddress(addressRequest, client);
            } else {
                updateExistingAddress(addressRequest, client);
            }
        });

        if (!addressesToRemove.isEmpty()) {
            client.getAddresses().removeAll(addressesToRemove);
            addressService.deleteMany(addressesToRemove);
        }
    }

    private void createAddress(AddressRequestDto addressRequest, Client client) {
        Address address = addressService.createAddress(addressRequest, client);

        if (Role.PARTNER.equals(client.getRole()) && address.getLatitude() == null && address.getLongitude() == null) {
            mapsService.getAddressCoordinates(address);
        }
    }

    private void updateExistingAddress(AddressRequestDto addressRequest, Client client) {
        Address address = addressService.findById(addressRequest.getId());

        if (!address.getClient().getId().equals(client.getId())) {
            throw new BusinessRuleException(AddressValidationMessages.ADDRESS_NOT_BELONG_TO_CLIENT);
        }

        if (!address.hasChanged(addressRequest)) {
            return;
        }

        boolean linkedToMonitor = monitorRepository.existsByAddressId(address.getId());
        BeanUtils.copyProperties(
                addressRequest,
                address,
                "latitude",
                "longitude",
                "client",
                "monitors",
                "locationName",
                "locationDescription",
                "photoUrl"
        );
        address.setUsernameUpdate(client.getBusinessName());

        if (linkedToMonitor || Role.PARTNER.equals(client.getRole())) {
            address.setLatitude(null);
            address.setLongitude(null);
            mapsService.getAddressCoordinates(address);
        }
    }
}
