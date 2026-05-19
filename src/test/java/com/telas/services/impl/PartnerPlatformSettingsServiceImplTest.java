package com.telas.services.impl;

import com.telas.entities.PlatformSettings;
import com.telas.repositories.PlatformSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerPlatformSettingsServiceImplTest {

    @Mock
    private PlatformSettingsRepository platformSettingsRepository;

    @InjectMocks
    private PartnerPlatformSettingsServiceImpl service;

    @Test
    void returnsFalseWhenRowMissing() {
        when(platformSettingsRepository.findById((short) 1)).thenReturn(Optional.empty());
        assertFalse(service.isSlotsAnyLocationEnabled());
    }

    @Test
    void persistsGlobalFlag() {
        PlatformSettings row = new PlatformSettings();
        row.setId((short) 1);
        row.setPartnerSlotsAnyLocationEnabled(false);
        when(platformSettingsRepository.findById((short) 1)).thenReturn(Optional.of(row));

        assertTrue(service.setSlotsAnyLocationEnabled(true));

        ArgumentCaptor<PlatformSettings> captor = ArgumentCaptor.forClass(PlatformSettings.class);
        verify(platformSettingsRepository).save(captor.capture());
        assertTrue(captor.getValue().isPartnerSlotsAnyLocationEnabled());
    }
}
