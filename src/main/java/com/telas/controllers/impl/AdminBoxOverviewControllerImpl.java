package com.telas.controllers.impl;

import com.telas.dtos.response.BoxOverviewDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.enums.Permission;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.services.AdminBoxOverviewService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("admin/box-overview")
@Tag(name = "Admin Box Overview", description = "Visual summary of all boxes with health and ad status")
@RequiredArgsConstructor
public class AdminBoxOverviewControllerImpl {

    private final AdminBoxOverviewService adminBoxOverviewService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    @Operation(summary = "Get a visual overview of all boxes including heartbeat, connectivity and active ads")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> listBoxOverview() {
        authenticatedUserService.validatePermission(Permission.MONITORING_BOX_OVERVIEW_VIEW);
        List<BoxOverviewDto> result = adminBoxOverviewService.listBoxOverview();
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(result, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }
}
