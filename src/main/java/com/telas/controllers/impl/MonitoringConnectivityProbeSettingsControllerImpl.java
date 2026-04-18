package com.telas.controllers.impl;

import com.telas.dtos.request.UpdateConnectivityProbeSettingsRequestDto;
import com.telas.dtos.response.ConnectivityProbeSettingsResponseDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.enums.Permission;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.services.MonitoringConnectivityProbeSettingsService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("monitoring/box-connectivity-probe/settings")
@Tag(name = "Monitoring connectivity probe settings", description = "Interval between connectivity probe runs")
@RequiredArgsConstructor
public class MonitoringConnectivityProbeSettingsControllerImpl {

    private final MonitoringConnectivityProbeSettingsService monitoringConnectivityProbeSettingsService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    @Operation(summary = "Get box connectivity probe interval (ms)")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> getSettings() {
        authenticatedUserService.validatePermission(Permission.MONITORING_CONNECTIVITY_PROBE_SETTINGS);
        long intervalMs = monitoringConnectivityProbeSettingsService.getIntervalMs();
        ConnectivityProbeSettingsResponseDto data =
                ConnectivityProbeSettingsResponseDto.builder().intervalMs(intervalMs).build();
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(data, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @PutMapping
    @Operation(summary = "Update box connectivity probe interval (ms)")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> updateSettings(@Valid @RequestBody UpdateConnectivityProbeSettingsRequestDto body) {
        authenticatedUserService.validatePermission(Permission.MONITORING_CONNECTIVITY_PROBE_SETTINGS);
        long intervalMs = monitoringConnectivityProbeSettingsService.setIntervalMs(body.getIntervalMs());
        ConnectivityProbeSettingsResponseDto data =
                ConnectivityProbeSettingsResponseDto.builder().intervalMs(intervalMs).build();
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(data, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }
}
