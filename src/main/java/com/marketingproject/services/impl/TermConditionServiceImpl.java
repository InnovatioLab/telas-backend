package com.marketingproject.services.impl;

import com.marketingproject.entities.TermCondition;
import com.marketingproject.repositories.TermConditionRepository;
import com.marketingproject.services.TermConditionService;
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
