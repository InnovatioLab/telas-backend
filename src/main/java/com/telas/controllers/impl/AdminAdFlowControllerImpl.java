package com.telas.controllers.impl;

import com.telas.dtos.response.AdFlowSummaryDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.enums.Permission;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.services.AdminAdFlowService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("admin/ad-flows")
@Tag(name = "Admin Ad Flows", description = "Ad request workflow timeline viewer (admin)")
@RequiredArgsConstructor
public class AdminAdFlowControllerImpl {

    private final AdminAdFlowService adminAdFlowService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    @Operation(summary = "List ad request workflow flows with timeline events")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> listFlows(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        authenticatedUserService.validatePermission(Permission.MONITORING_AD_FLOW_VIEW);
        Page<AdFlowSummaryDto> result = adminAdFlowService.listFlows(
                clientId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(result, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }
}
