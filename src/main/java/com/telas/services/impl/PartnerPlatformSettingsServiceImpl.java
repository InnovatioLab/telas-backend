package com.telas.services.impl;

import com.telas.entities.PlatformSettings;
import com.telas.repositories.PlatformSettingsRepository;
import com.telas.services.PartnerPlatformSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartnerPlatformSettingsServiceImpl implements PartnerPlatformSettingsService {

    private static final short SETTINGS_ROW_ID = 1;

    private final PlatformSettingsRepository platformSettingsRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isSlotsAnyLocationEnabled() {
        return platformSettingsRepository
                .findById(SETTINGS_ROW_ID)
                .map(PlatformSettings::isPartnerSlotsAnyLocationEnabled)
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean setSlotsAnyLocationEnabled(boolean enabled) {
        PlatformSettings row =
                platformSettingsRepository
                        .findById(SETTINGS_ROW_ID)
                        .orElseGet(
                                () -> {
                                    PlatformSettings created = new PlatformSettings();
                                    created.setId(SETTINGS_ROW_ID);
                                    return created;
                                });
        row.setPartnerSlotsAnyLocationEnabled(enabled);
        platformSettingsRepository.save(row);
        return enabled;
    }
}
