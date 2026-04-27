package com.telas.controllers.impl;

import com.telas.dtos.request.SmartPlugAccountCreateRequestDto;
import com.telas.dtos.request.SmartPlugAccountUpdateRequestDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.dtos.response.SmartPlugAccountResponseDto;
import com.telas.enums.Permission;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.services.SmartPlugAccountAdminService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("monitoring/smart-plug-accounts")
@Tag(
        name = "Smart plug accounts",
        description = "Default Kasa/Tapo credentials per box (used when a plug has no dedicated account)")
@RequiredArgsConstructor
public class SmartPlugAccountAdminController {

    private final SmartPlugAccountAdminService smartPlugAccountAdminService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    @Operation(summary = "Lista contas padrão por box")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> list(@RequestParam UUID boxId) {
        authenticatedUserService.validatePermission(Permission.MONITORING_SMART_PLUG_VIEW);
        List<SmartPlugAccountResponseDto> list = smartPlugAccountAdminService.listByBox(boxId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(list, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtém conta por id")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        authenticatedUserService.validatePermission(Permission.MONITORING_SMART_PLUG_VIEW);
        SmartPlugAccountResponseDto data = smartPlugAccountAdminService.getById(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(data, HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
    }

    @PostMapping
    @Operation(summary = "Cria conta padrão (box + vendor únicos)")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> create(@Valid @RequestBody SmartPlugAccountCreateRequestDto dto) {
        authenticatedUserService.validatePermission(Permission.MONITORING_SMART_PLUG_ACCOUNTS_MANAGE);
        SmartPlugAccountResponseDto data = smartPlugAccountAdminService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(data, HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza conta (senha opcional; vazio mantém a atual)")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> update(
            @PathVariable UUID id, @Valid @RequestBody SmartPlugAccountUpdateRequestDto dto) {
        authenticatedUserService.validatePermission(Permission.MONITORING_SMART_PLUG_ACCOUNTS_MANAGE);
        SmartPlugAccountResponseDto data = smartPlugAccountAdminService.update(id, dto);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(data, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove conta")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        authenticatedUserService.validatePermission(Permission.MONITORING_SMART_PLUG_ACCOUNTS_MANAGE);
        smartPlugAccountAdminService.delete(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.DELETE_SUCCESS_MESSAGE));
    }
}
