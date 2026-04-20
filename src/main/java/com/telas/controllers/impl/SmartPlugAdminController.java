package com.telas.controllers.impl;

import com.telas.dtos.request.SmartPlugInventoryRequestDto;
import com.telas.dtos.request.SmartPlugRequestDto;
import com.telas.dtos.request.SmartPlugUpdateRequestDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.dtos.response.SmartPlugHistoryPointResponseDto;
import com.telas.dtos.response.SmartPlugReadingResponseDto;
import com.telas.dtos.response.SmartPlugOverviewResponseDto;
import com.telas.dtos.response.SmartPlugResponseDto;
import com.telas.enums.Permission;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.services.SmartPlugOverviewService;
import com.telas.services.SmartPlugAdminService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("monitoring/smart-plugs")
@Tag(name = "Smart plugs", description = "Tomadas inteligentes por monitor (admin)")
@RequiredArgsConstructor
public class SmartPlugAdminController {

    private final SmartPlugAdminService smartPlugAdminService;
    private final SmartPlugOverviewService smartPlugOverviewService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    @Operation(summary = "Lista tomadas configuradas")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> list() {
        authenticatedUserService.validatePermission(Permission.MONITORING_SMART_PLUG_VIEW);
        List<SmartPlugResponseDto> list = smartPlugAdminService.findAll();
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(list, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @GetMapping("/overview")
    @Operation(summary = "Visão operacional: tomadas + última leitura")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> overview() {
        authenticatedUserService.validatePermission(Permission.MONITORING_SMART_PLUG_VIEW);
        List<SmartPlugOverviewResponseDto> list = smartPlugOverviewService.overview();
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(list, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Histórico de leituras (check_runs) por tomada")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> history(
            @PathVariable UUID id,
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to,
            @RequestParam(name = "limit", required = false, defaultValue = "200") int limit) {
        authenticatedUserService.validatePermission(Permission.MONITORING_SMART_PLUG_VIEW);
        List<SmartPlugHistoryPointResponseDto> list = smartPlugOverviewService.history(id, from, to, limit);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(list, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @GetMapping("/unassigned")
    @Operation(summary = "Lista inventário e opcionalmente a tomada já ligada ao monitor indicado")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> listUnassigned(
            @RequestParam(name = "forMonitorId", required = false) UUID forMonitorId,
            @RequestParam(name = "forBoxId", required = false) UUID forBoxId) {
        authenticatedUserService.validateAdmin();
        List<SmartPlugResponseDto> list =
                smartPlugAdminService.findUnassignedInventory(forMonitorId, forBoxId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(list, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @PostMapping
    @Operation(
            summary = "Regista tomada e associa a um monitor",
            description = "Apenas DEVELOPER: criação já associada a um ecrã.")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> create(@Valid @RequestBody SmartPlugRequestDto dto) {
        authenticatedUserService.validateDeveloper();
        SmartPlugResponseDto data = smartPlugAdminService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(data, HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
    }

    @PostMapping("/unassigned")
    @Operation(
            summary = "Regista tomada em inventário (sem monitor)",
            description = "Apenas DEVELOPER.")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> createInventory(@Valid @RequestBody SmartPlugInventoryRequestDto dto) {
        authenticatedUserService.validateDeveloper();
        SmartPlugResponseDto data = smartPlugAdminService.createInventory(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(data, HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
    }

    @PutMapping("/{plugId}/assign/{monitorId}")
    @Operation(summary = "Associa ou move tomada para um ecrã", description = "Apenas DEVELOPER.")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> assign(
            @PathVariable UUID plugId, @PathVariable UUID monitorId) {
        authenticatedUserService.validateDeveloper();
        SmartPlugResponseDto data = smartPlugAdminService.assignToMonitor(plugId, monitorId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(data, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @PutMapping("/{plugId}/assign-box/{boxId}")
    @Operation(summary = "Associa ou move tomada para uma box", description = "Apenas DEVELOPER.")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> assignToBox(
            @PathVariable UUID plugId, @PathVariable UUID boxId) {
        authenticatedUserService.validateDeveloper();
        SmartPlugResponseDto data = smartPlugAdminService.assignToBox(plugId, boxId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(data, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @PutMapping("/{plugId}/unassign")
    @Operation(summary = "Devolve tomada ao inventário", description = "Apenas DEVELOPER.")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> unassign(@PathVariable UUID plugId) {
        authenticatedUserService.validateDeveloper();
        SmartPlugResponseDto data = smartPlugAdminService.unassign(plugId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(data, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza metadados da tomada (sem alterar associação por aqui)")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody SmartPlugUpdateRequestDto dto) {
        authenticatedUserService.validatePermission(Permission.MONITORING_SMART_PLUG_ADMIN);
        SmartPlugResponseDto data = smartPlugAdminService.update(id, dto);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(data, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove tomada")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        authenticatedUserService.validatePermission(Permission.MONITORING_SMART_PLUG_ADMIN);
        smartPlugAdminService.delete(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.DELETE_SUCCESS_MESSAGE));
    }

    @PostMapping("/{id}/test-read")
    @Operation(summary = "Testa leitura da tomada (stub ou sidecar)")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> testRead(@PathVariable UUID id) {
        authenticatedUserService.validatePermission(Permission.MONITORING_TESTING_EXECUTE);
        SmartPlugReadingResponseDto data = smartPlugAdminService.testRead(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(data, HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
    }
}
