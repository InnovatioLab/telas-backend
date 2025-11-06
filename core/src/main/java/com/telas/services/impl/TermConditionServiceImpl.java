package com.telas.services.impl;

import com.telas.dtos.response.TermConditionResponseDto;
import com.telas.entities.TermCondition;
import com.telas.repositories.TermConditionRepository;
import com.telas.services.TermConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TermConditionServiceImpl implements TermConditionService {
  private final TermConditionRepository termConditionRepository;

  @Override
  @Transactional(readOnly = true)
  public TermConditionResponseDto getActualTermCondition() {
    return new TermConditionResponseDto(termConditionRepository.findLatest());
  }

  @Override
  @Transactional(readOnly = true)
  public TermCondition getLastTermCondition() {
    return termConditionRepository.findLatest();
  }
}
