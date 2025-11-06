package com.telas.services;

import com.telas.dtos.response.TermConditionResponseDto;
import com.telas.entities.TermCondition;

public interface TermConditionService {
  TermConditionResponseDto getActualTermCondition();

  TermCondition getLastTermCondition();
}
