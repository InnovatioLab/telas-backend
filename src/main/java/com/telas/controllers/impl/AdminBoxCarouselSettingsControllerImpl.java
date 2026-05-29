package com.telas.controllers.impl;

import com.telas.dtos.request.UpdateBoxCarouselSettingsRequestDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.services.BoxCarouselSettingsService;
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
@RequestMapping("admin/box-carousel-settings")
@Tag(name = "Box carousel settings", description = "Tempos de exibição e transição do player na box")
@RequiredArgsConstructor
public class AdminBoxCarouselSettingsControllerImpl {

    private final BoxCarouselSettingsService boxCarouselSettingsService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    @Operation(summary = "Obtém configuração global do carrossel da box")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> getSettings() {
        authenticatedUserService.validateAdmin();
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                boxCarouselSettingsService.getSettings(),
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @PutMapping
    @Operation(summary = "Atualiza configuração global do carrossel da box")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> updateSettings(@Valid @RequestBody UpdateBoxCarouselSettingsRequestDto body) {
        authenticatedUserService.validateAdmin();
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                boxCarouselSettingsService.updateSettings(body),
                                HttpStatus.OK,
                                MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }
}
