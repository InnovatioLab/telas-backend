package com.telas.services.impl;

import com.telas.dtos.response.PrivacyPolicyResponseDto;
import com.telas.repositories.PrivacyPolicyRepository;
import com.telas.services.PrivacyPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrivacyPolicyServiceImpl implements PrivacyPolicyService {
  private final PrivacyPolicyRepository repository;

  @Override
  @Transactional(readOnly = true)
  public PrivacyPolicyResponseDto getActualPolicyPrivacy() {
    return new PrivacyPolicyResponseDto(repository.findLatest());
  }
}
