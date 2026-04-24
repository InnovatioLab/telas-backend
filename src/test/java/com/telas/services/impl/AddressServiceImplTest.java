package com.telas.services.impl;

import com.telas.dtos.request.AddressRequestDto;
import com.telas.entities.Address;
import com.telas.entities.Client;
import com.telas.enums.DefaultStatus;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.repositories.AddressRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AddressServiceImplTest {

    @Test
    void createAddress_whenActiveClientConflictExistsForDifferentClient_throws() {
        AddressRepository repo = mock(AddressRepository.class);
        AddressServiceImpl service = new AddressServiceImpl(repo);

        Client existingClient = new Client();
        existingClient.setId(UUID.randomUUID());
        existingClient.setStatus(DefaultStatus.ACTIVE);

        Address existing = new Address();
        existing.setClient(existingClient);

        when(repo.findActiveClientConflictByAddressData(any(), any(), any(), any(), any(), eq(DefaultStatus.ACTIVE)))
            .thenReturn(Optional.of(existing));

        Client newClient = new Client();
        newClient.setId(UUID.randomUUID());
        newClient.setStatus(DefaultStatus.ACTIVE);

        AddressRequestDto dto = new AddressRequestDto();
        dto.setStreet("1 main st");
        dto.setCity("Austin");
        dto.setState("TX");
        dto.setZipCode("78701");

        assertThrows(BusinessRuleException.class, () -> service.createAddress(dto, newClient));
    }

    @Test
    void createAddress_whenActiveClientConflictExistsForSameClient_returnsExisting() {
        AddressRepository repo = mock(AddressRepository.class);
        AddressServiceImpl service = new AddressServiceImpl(repo);

        UUID clientId = UUID.randomUUID();
        Client client = new Client();
        client.setId(clientId);
        client.setStatus(DefaultStatus.ACTIVE);

        Address existing = new Address();
        existing.setClient(client);

        when(repo.findActiveClientConflictByAddressData(any(), any(), any(), any(), any(), eq(DefaultStatus.ACTIVE)))
            .thenReturn(Optional.of(existing));

        AddressRequestDto dto = new AddressRequestDto();
        dto.setStreet("1 main st");
        dto.setCity("Austin");
        dto.setState("TX");
        dto.setZipCode("78701");

        Address result = service.createAddress(dto, client);

        assertEquals(existing, result);
    }

    @Test
    void createAddress_whenNoActiveClientConflict_createsNewAddressForClient() {
        AddressRepository repo = mock(AddressRepository.class);
        AddressServiceImpl service = new AddressServiceImpl(repo);

        when(repo.findActiveClientConflictByAddressData(any(), any(), any(), any(), any(), eq(DefaultStatus.ACTIVE)))
            .thenReturn(Optional.empty());

        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setStatus(DefaultStatus.ACTIVE);

        AddressRequestDto dto = new AddressRequestDto();
        dto.setStreet("1 main st");
        dto.setCity("Austin");
        dto.setState("TX");
        dto.setZipCode("78701");

        Address result = service.createAddress(dto, client);

        assertNotNull(result);
        assertEquals(client, result.getClient());
    }

    @Test
    void createAddress_whenSameStreetCityStateZipButDifferentAddress2_resolvesNoConflict() {
        AddressRepository repo = mock(AddressRepository.class);
        AddressServiceImpl service = new AddressServiceImpl(repo);

        when(repo.findActiveClientConflictByAddressData(
                eq("1 main st"), eq("austin"), eq("tx"), eq("78701"), eq("suite b"), eq(DefaultStatus.ACTIVE)))
            .thenReturn(Optional.empty());

        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setStatus(DefaultStatus.ACTIVE);

        AddressRequestDto dto = new AddressRequestDto();
        dto.setStreet("1 main st");
        dto.setCity("Austin");
        dto.setState("TX");
        dto.setZipCode("78701");
        dto.setAddress2("  Suite B  ");

        Address result = service.createAddress(dto, client);

        assertNotNull(result);
        assertEquals(client, result.getClient());
        verify(repo).findActiveClientConflictByAddressData(
                eq("1 main st"), eq("austin"), eq("tx"), eq("78701"), eq("suite b"), eq(DefaultStatus.ACTIVE));
    }
}

