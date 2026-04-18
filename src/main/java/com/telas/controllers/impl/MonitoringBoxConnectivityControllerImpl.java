package com.telas.controllers.impl;

import com.telas.dtos.response.BoxConnectivityProbeRowResponseDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.enums.Permission;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.services.BoxConnectivityProbeService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("monitoring/box-connectivity-probes")
@Tag(name = "Monitoring box connectivity", description = "Last TCP connectivity probe per box (admin)")
@RequiredArgsConstructor
public class MonitoringBoxConnectivityControllerImpl {

    private final BoxConnectivityProbeService boxConnectivityProbeService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    @Operation(summary = "List last connectivity probe per box/monitor row")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> list() {
        authenticatedUserService.validatePermission(Permission.MONITORING_BOX_PING_VIEW);
        List<BoxConnectivityProbeRowResponseDto> data = boxConnectivityProbeService.listProbeRows();
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                data,
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @PostMapping("/run")
    @Operation(summary = "Run TCP connectivity probes now and return the same list as GET")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> runNow() {
        authenticatedUserService.validatePermission(Permission.MONITORING_BOX_PING_VIEW);
        boxConnectivityProbeService.runProbesNow();
        List<BoxConnectivityProbeRowResponseDto> data = boxConnectivityProbeService.listProbeRows();
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                data,
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }
}
