package com.telas.services.impl;

import com.telas.entities.TermCondition;
import com.telas.repositories.TermConditionRepository;
import com.telas.services.TermConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TermConditionServiceImpl implements TermConditionService {
    private final TermConditionRepository termConditionRepository;

    @Override
    public TermCondition getActualTermCondition() {
        return termConditionRepository.findLatest();
    }
}
