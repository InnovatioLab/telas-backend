package com.telas.controllers.impl;

import com.telas.controllers.PrivacyPolicyController;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.PrivacyPolicyService;
import com.telas.shared.constants.MessageCommonsConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "privacy-policy")
@RequiredArgsConstructor
public class PrivacyPolicyControllerImpl implements PrivacyPolicyController {
  private final PrivacyPolicyService service;

  @Override
  @GetMapping
  public ResponseEntity<?> getPolicyPrivacy() {
    return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseDto.fromData(service.getActualPolicyPrivacy(), HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
  }
}
