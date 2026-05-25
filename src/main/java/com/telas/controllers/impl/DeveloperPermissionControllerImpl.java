package com.telas.controllers.impl;

import com.telas.dtos.request.UpdateEmailAlertPreferencesRequestDto;
import com.telas.dtos.request.UpdatePartnerPlatformSettingsRequestDto;
import com.telas.dtos.request.UpdatePermissionsRequestDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.services.DeveloperAdminService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("developer")
@Tag(name = "Developer permissions", description = "Gestão de permissões por admin (apenas DEVELOPER)")
@RequiredArgsConstructor
public class DeveloperPermissionControllerImpl {

    private final AuthenticatedUserService authenticatedUserService;
    private final DeveloperAdminService developerAdminService;

    @GetMapping("/admins")
    @Operation(summary = "Lista admins com permissões de monitorização")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> listAdminsWithPermissions() {
        authenticatedUserService.validateDeveloper();
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                developerAdminService.listAdminsWithPermissions(),
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @GetMapping("/partner-platform-settings")
    @Operation(summary = "Configuração global de quota de partners em qualquer tela")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> getPartnerPlatformSettings() {
        authenticatedUserService.validateDeveloper();
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                developerAdminService.getPartnerPlatformSettings(),
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @PutMapping("/partner-platform-settings")
    @Operation(summary = "Atualiza configuração global de quota de partners em qualquer tela")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> updatePartnerPlatformSettings(
            @Valid @RequestBody UpdatePartnerPlatformSettingsRequestDto body) {
        authenticatedUserService.validateDeveloper();
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                developerAdminService.updatePartnerPlatformSettings(body),
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @PutMapping("/clients/{clientId}/permissions")
    @Operation(summary = "Substitui o conjunto de permissões de um admin")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> replacePermissions(
            @PathVariable UUID clientId, @Valid @RequestBody UpdatePermissionsRequestDto body) {
        var dev = authenticatedUserService.validateDeveloper();
        developerAdminService.replacePermissions(clientId, body, dev.client().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissions/catalog")
    @Operation(summary = "Lista códigos de permissão disponíveis")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> permissionCatalog() {
        authenticatedUserService.validateDeveloper();
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                developerAdminService.permissionCatalog(),
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @GetMapping("/email-alert-preferences/catalog")
    @Operation(summary = "Email alert categories (English labels) for admin notifications")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> emailAlertCatalog() {
        authenticatedUserService.validateDeveloper();
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                developerAdminService.emailAlertCatalog(),
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @GetMapping("/clients/{clientId}/email-alert-preferences")
    @Operation(summary = "Email alert preferences for an admin user")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> getEmailAlertPreferences(@PathVariable UUID clientId) {
        authenticatedUserService.validateDeveloper();
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                developerAdminService.getEmailAlertPreferences(clientId),
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @PutMapping("/clients/{clientId}/email-alert-preferences")
    @Operation(summary = "Replace email alert preferences for an admin user")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> replaceEmailAlertPreferences(
            @PathVariable UUID clientId, @Valid @RequestBody UpdateEmailAlertPreferencesRequestDto body) {
        authenticatedUserService.validateDeveloper();
        developerAdminService.replaceEmailAlertPreferences(clientId, body);
        return ResponseEntity.noContent().build();
    }
}
