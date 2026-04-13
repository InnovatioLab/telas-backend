package com.telas.controllers.impl;

import com.telas.dtos.request.SmartPlugRequestDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.dtos.response.SmartPlugReadingResponseDto;
import com.telas.dtos.response.SmartPlugResponseDto;
import com.telas.enums.Permission;
import com.telas.infra.security.services.AuthenticatedUserService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("monitoring/smart-plugs")
@Tag(name = "Smart plugs", description = "Tomadas inteligentes por monitor (admin)")
@RequiredArgsConstructor
public class SmartPlugAdminController {

    private final SmartPlugAdminService smartPlugAdminService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    @Operation(summary = "Lista tomadas configuradas")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> list() {
        authenticatedUserService.validateAdmin();
        List<SmartPlugResponseDto> list = smartPlugAdminService.findAll();
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(list, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @PostMapping
    @Operation(summary = "Regista tomada e associa a um monitor")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> create(@Valid @RequestBody SmartPlugRequestDto dto) {
        authenticatedUserService.validatePermission(Permission.MONITORING_SMART_PLUG_ADMIN);
        SmartPlugResponseDto data = smartPlugAdminService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(data, HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza tomada")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody SmartPlugRequestDto dto) {
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
