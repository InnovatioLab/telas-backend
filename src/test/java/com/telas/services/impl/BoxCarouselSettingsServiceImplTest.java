package com.telas.services.impl;

import com.telas.dtos.request.UpdateBoxCarouselSettingsRequestDto;
import com.telas.entities.BoxCarouselSettings;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.repositories.BoxCarouselSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoxCarouselSettingsServiceImplTest {

    @Mock
    private BoxCarouselSettingsRepository repository;

    @InjectMocks
    private BoxCarouselSettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultDisplayDurationMs", 5000);
        ReflectionTestUtils.setField(service, "defaultTransitionDurationMs", 2000);
        ReflectionTestUtils.setField(service, "defaultTotalSlots", (short) 15);
    }

    @Test
    void getSettings_returnsDefaultsWhenRowMissing() {
        when(repository.findById((short) 1)).thenReturn(Optional.empty());

        var settings = service.getSettings();

        assertEquals(5000, settings.getDisplayDurationMs());
        assertEquals(2000, settings.getTransitionDurationMs());
        assertEquals(15, settings.getTotalSlots());
    }

    @Test
    void updateSettings_persistsRow() {
        when(repository.findById((short) 1)).thenReturn(Optional.empty());
        when(repository.save(any(BoxCarouselSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateBoxCarouselSettingsRequestDto();
        request.setDisplayDurationMs(6000);
        request.setTransitionDurationMs(1500);
        request.setTotalSlots(15);

        var result = service.updateSettings(request);

        assertEquals(6000, result.getDisplayDurationMs());
        assertEquals(1500, result.getTransitionDurationMs());
        verify(repository).save(any(BoxCarouselSettings.class));
    }

    @Test
    void updateSettings_rejectsInvalidDisplayDuration() {
        var request = new UpdateBoxCarouselSettingsRequestDto();
        request.setDisplayDurationMs(500);
        request.setTransitionDurationMs(2000);
        request.setTotalSlots(15);

        assertThrows(BusinessRuleException.class, () -> service.updateSettings(request));
    }
}
