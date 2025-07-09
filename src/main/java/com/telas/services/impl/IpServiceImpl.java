package com.telas.services.impl;

import com.telas.dtos.response.IpResponseDto;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.IpRepository;
import com.telas.services.IpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IpServiceImpl implements IpService {
  private final IpRepository repository;
  private final AuthenticatedUserService authenticatedUserService;

  @Override
  @Transactional(readOnly = true)
  public List<IpResponseDto> findAll() {
    authenticatedUserService.validateAdmin();

    return repository.findAll().stream()
            .map(IpResponseDto::new)
            .toList();
  }
}
