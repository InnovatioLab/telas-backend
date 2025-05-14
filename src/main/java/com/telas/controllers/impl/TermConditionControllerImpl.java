package com.telas.controllers.impl;

import com.telas.controllers.TermConditionController;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.TermConditionService;
import com.telas.shared.constants.MessageCommonsConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "terms_conditions")
@RequiredArgsConstructor
public class TermConditionControllerImpl implements TermConditionController {
    private final TermConditionService service;

    @Override
    public ResponseEntity<?> getTerms() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(service.getActualTermCondition(), HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }
}
