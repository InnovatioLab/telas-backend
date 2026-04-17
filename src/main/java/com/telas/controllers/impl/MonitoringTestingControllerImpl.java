package com.telas.controllers.impl;

import com.telas.dtos.response.BoxHeartbeatCheckResponseDto;
import com.telas.dtos.response.MonitoringTestingRowDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.enums.Permission;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.services.BoxScriptUpdateCommandService;
import com.telas.services.MonitoringTestingService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("monitoring/testing")
@Tag(name = "Monitoring testing", description = "Overview e verificação de heartbeat (admin)")
@RequiredArgsConstructor
public class MonitoringTestingControllerImpl {

    private final MonitoringTestingService monitoringTestingService;
    private final BoxScriptUpdateCommandService boxScriptUpdateCommandService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/overview")
    @Operation(summary = "Lista boxes, monitores, tomadas e estado do heartbeat")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> overview() {
        authenticatedUserService.validateAdmin();
        List<MonitoringTestingRowDto> data = monitoringTestingService.getOverview();
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                data,
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @PostMapping("/boxes/{boxId}/check")
    @Operation(summary = "Verifica último heartbeat da box (lógico, não ICMP)")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> checkBox(@PathVariable UUID boxId) {
        authenticatedUserService.validatePermission(Permission.MONITORING_TESTING_EXECUTE);
        BoxHeartbeatCheckResponseDto data = monitoringTestingService.checkBoxHeartbeat(boxId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                data,
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
    }

    @PostMapping("/boxes/{boxId}/box-script-update")
    @Operation(summary = "Enfileira atualização do box-script (artefacto configurado no servidor)")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> enqueueBoxScriptUpdate(@PathVariable UUID boxId) {
        authenticatedUserService.validatePermission(Permission.MONITORING_TESTING_EXECUTE);
        boxScriptUpdateCommandService.enqueue(boxId);
        return ResponseEntity.noContent().build();
    }
}
