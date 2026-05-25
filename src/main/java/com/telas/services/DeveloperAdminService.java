package com.telas.services;

import com.telas.dtos.request.UpdateEmailAlertPreferencesRequestDto;
import com.telas.dtos.request.UpdatePartnerPlatformSettingsRequestDto;
import com.telas.dtos.request.UpdatePermissionsRequestDto;
import com.telas.dtos.response.AdminPermissionRowResponseDto;
import com.telas.dtos.response.EmailAlertCategoryOptionDto;
import com.telas.dtos.response.EmailAlertPreferencesResponseDto;
import com.telas.dtos.response.PartnerPlatformSettingsResponseDto;

import java.util.List;
import java.util.UUID;

public interface DeveloperAdminService {

    List<AdminPermissionRowResponseDto> listAdminsWithPermissions();

    PartnerPlatformSettingsResponseDto getPartnerPlatformSettings();

    PartnerPlatformSettingsResponseDto updatePartnerPlatformSettings(UpdatePartnerPlatformSettingsRequestDto body);

    void replacePermissions(UUID clientId, UpdatePermissionsRequestDto body, UUID actorDeveloperId);

    List<String> permissionCatalog();

    List<EmailAlertCategoryOptionDto> emailAlertCatalog();

    EmailAlertPreferencesResponseDto getEmailAlertPreferences(UUID clientId);

    void replaceEmailAlertPreferences(UUID clientId, UpdateEmailAlertPreferencesRequestDto body);
}
