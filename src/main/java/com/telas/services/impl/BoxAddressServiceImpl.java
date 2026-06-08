package com.telas.services.impl;

import com.telas.dtos.request.BoxAddressRequestDto;
import com.telas.dtos.response.BoxAddressResponseDto;
import com.telas.entities.BoxAddress;
import com.telas.enums.Permission;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.BoxAddressRepository;
import com.telas.services.BoxAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoxAddressServiceImpl implements BoxAddressService {
  private final BoxAddressRepository repository;
  private final AuthenticatedUserService authenticatedUserService;

  @Override
  @Transactional(readOnly = true)
  public List<BoxAddressResponseDto> findAll() {
    authenticatedUserService.validateAdmin();

    return repository.findAllAvailable().stream()
            .map(BoxAddressResponseDto::new)
            .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<BoxAddressResponseDto> findAllForAdmin() {
    authenticatedUserService.validatePrivilegedPanelOrPermission(Permission.ADMIN_BOX_ADDRESS_VIEW);

    return repository.findAll().stream()
            .map(BoxAddressResponseDto::new)
            .toList();
  }

  @Override
  @Transactional
  public BoxAddressResponseDto create(BoxAddressRequestDto dto) {
    authenticatedUserService.validatePrivilegedPanelOrPermission(Permission.ADMIN_BOX_ADDRESS_CREATE);

    BoxAddress address = new BoxAddress();
    address.setIp(dto.getIp().trim());
    address.setMac(dto.getMac().trim());
    address.setDns(dto.getDns() != null && !dto.getDns().isBlank() ? dto.getDns().trim() : null);
    return new BoxAddressResponseDto(repository.save(address));
  }

  @Override
  @Transactional
  public BoxAddressResponseDto update(UUID id, BoxAddressRequestDto dto) {
    authenticatedUserService.validatePrivilegedPanelOrPermission(Permission.ADMIN_BOX_ADDRESS_MANAGE);

    BoxAddress address = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Box address not found"));
    address.setIp(dto.getIp().trim());
    address.setMac(dto.getMac().trim());
    address.setDns(dto.getDns() != null && !dto.getDns().isBlank() ? dto.getDns().trim() : null);
    return new BoxAddressResponseDto(repository.save(address));
  }

  @Override
  @Transactional
  public void delete(UUID id) {
    authenticatedUserService.validatePrivilegedPanelOrPermission(Permission.ADMIN_BOX_ADDRESS_MANAGE);

    BoxAddress address = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Box address not found"));
    if (address.getBox() != null) {
      throw new BusinessRuleException("Cannot remove a box address that is currently in use by a box");
    }
    repository.delete(address);
  }
}
