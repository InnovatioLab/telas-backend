package com.telas.controllers.impl;

import com.telas.dtos.request.UpdateEmailAlertPreferencesRequestDto;
import com.telas.dtos.request.UpdatePermissionsRequestDto;
import com.telas.dtos.response.AdminPermissionRowResponseDto;
import com.telas.dtos.response.EmailAlertPreferencesResponseDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.entities.Client;
import com.telas.enums.Permission;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.ClientRepository;
import com.telas.services.AdminEmailAlertPreferenceService;
import com.telas.services.PermissionService;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("developer")
@Tag(name = "Developer permissions", description = "Gestão de permissões por admin (apenas DEVELOPER)")
@RequiredArgsConstructor
public class DeveloperPermissionControllerImpl {

    private final AuthenticatedUserService authenticatedUserService;
    private final ClientRepository clientRepository;
    private final PermissionService permissionService;
    private final AdminEmailAlertPreferenceService adminEmailAlertPreferenceService;

    @GetMapping("/admins")
    @Operation(summary = "Lista admins com permissões de monitorização")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> listAdminsWithPermissions() {
        authenticatedUserService.validateDeveloper();
        List<Client> admins = clientRepository.findAllAdmins();
        List<AdminPermissionRowResponseDto> rows =
                admins.stream()
                        .map(
                                a ->
                                        new AdminPermissionRowResponseDto(
                                                a.getId(),
                                                a.getBusinessName(),
                                                a.getContact() != null
                                                        ? a.getContact().getEmail()
                                                        : "",
                                                permissionService.listPermissionCodesForClient(a.getId())))
                        .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                rows,
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @PutMapping("/clients/{clientId}/permissions")
    @Operation(summary = "Substitui o conjunto de permissões de um admin")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> replacePermissions(
            @PathVariable UUID clientId, @Valid @RequestBody UpdatePermissionsRequestDto body) {
        AuthenticatedUser dev = authenticatedUserService.validateDeveloper();
        Set<Permission> parsed = new HashSet<>();
        List<String> raw = body.getPermissions();
        if (raw != null) {
            for (String code : raw) {
                parsed.add(Permission.valueOf(code.trim()));
            }
        }
        permissionService.replacePermissionsForAdmin(clientId, parsed, dev.client().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissions/catalog")
    @Operation(summary = "Lista códigos de permissão disponíveis")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> permissionCatalog() {
        authenticatedUserService.validateDeveloper();
        List<String> codes =
                java.util.Arrays.stream(Permission.values())
                        .map(Enum::name)
                        .sorted()
                        .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                codes,
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
                                adminEmailAlertPreferenceService.getCatalog(),
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @GetMapping("/clients/{clientId}/email-alert-preferences")
    @Operation(summary = "Email alert preferences for an admin user")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> getEmailAlertPreferences(@PathVariable UUID clientId) {
        authenticatedUserService.validateDeveloper();
        EmailAlertPreferencesResponseDto data =
                adminEmailAlertPreferenceService.getPreferencesResponseForAdmin(clientId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                data,
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @PutMapping("/clients/{clientId}/email-alert-preferences")
    @Operation(summary = "Replace email alert preferences for an admin user")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> replaceEmailAlertPreferences(
            @PathVariable UUID clientId, @Valid @RequestBody UpdateEmailAlertPreferencesRequestDto body) {
        authenticatedUserService.validateDeveloper();
        adminEmailAlertPreferenceService.replaceFromRequest(clientId, body.getPreferences());
        return ResponseEntity.noContent().build();
    }
}
