package com.telas.services.impl;

import com.telas.dtos.request.UpdateBoxCarouselSettingsRequestDto;
import com.telas.dtos.response.BoxPlayerSettingsResponseDto;
import com.telas.entities.BoxCarouselSettings;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.repositories.BoxCarouselSettingsRepository;
import com.telas.services.BoxCarouselSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoxCarouselSettingsServiceImpl implements BoxCarouselSettingsService {

    private static final short ROW_ID = 1;

    private final BoxCarouselSettingsRepository repository;

    @Value("${box.carousel.display-duration-ms:5000}")
    private int defaultDisplayDurationMs;

    @Value("${box.carousel.transition-duration-ms:2000}")
    private int defaultTransitionDurationMs;

    @Value("${box.carousel.total-slots:15}")
    private short defaultTotalSlots;

    @Override
    @Transactional(readOnly = true)
    public BoxPlayerSettingsResponseDto getSettings() {
        return repository.findById(ROW_ID).map(BoxPlayerSettingsResponseDto::fromEntity).orElseGet(this::defaultSettings);
    }

    @Override
    @Transactional
    public BoxPlayerSettingsResponseDto updateSettings(UpdateBoxCarouselSettingsRequestDto request) {
        validate(request);
        BoxCarouselSettings row =
                repository.findById(ROW_ID).orElseGet(() -> {
                    BoxCarouselSettings created = new BoxCarouselSettings();
                    created.setId(ROW_ID);
                    return created;
                });
        row.setDisplayDurationMs(request.getDisplayDurationMs());
        row.setTransitionDurationMs(request.getTransitionDurationMs());
        row.setTotalSlots(request.getTotalSlots().shortValue());
        repository.save(row);
        return BoxPlayerSettingsResponseDto.fromEntity(row);
    }

    private BoxPlayerSettingsResponseDto defaultSettings() {
        return new BoxPlayerSettingsResponseDto(
                defaultDisplayDurationMs, defaultTransitionDurationMs, defaultTotalSlots);
    }

    private static void validate(UpdateBoxCarouselSettingsRequestDto request) {
        if (request.getDisplayDurationMs() < 1000 || request.getDisplayDurationMs() > 60000) {
            throw new BusinessRuleException("displayDurationMs must be between 1000 and 60000");
        }
        if (request.getTransitionDurationMs() < 0 || request.getTransitionDurationMs() > 10000) {
            throw new BusinessRuleException("transitionDurationMs must be between 0 and 10000");
        }
        if (request.getTotalSlots() < 1 || request.getTotalSlots() > 30) {
            throw new BusinessRuleException("totalSlots must be between 1 and 30");
        }
    }
}
