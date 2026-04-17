package com.telas.services;

import com.telas.dtos.response.EmailAlertCategoryOptionDto;
import com.telas.dtos.response.EmailAlertPreferencesResponseDto;
import com.telas.enums.AdminEmailAlertCategory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AdminEmailAlertPreferenceService {

    boolean wantsEmail(UUID clientId, AdminEmailAlertCategory category);

    EnumMap<AdminEmailAlertCategory, Boolean> getPreferencesForAdmin(UUID clientId);

    EmailAlertPreferencesResponseDto getPreferencesResponseForAdmin(UUID clientId);

    void replacePreferencesForAdmin(UUID targetClientId, EnumMap<AdminEmailAlertCategory, Boolean> preferences);

    void replaceFromRequest(UUID targetClientId, Map<String, Boolean> raw);

    List<EmailAlertCategoryOptionDto> getCatalog();
}
