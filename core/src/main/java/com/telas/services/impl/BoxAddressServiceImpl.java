package com.telas.services.impl;

import com.telas.dtos.response.BoxAddressResponseDto;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.BoxAddressRepository;
import com.telas.services.BoxAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
