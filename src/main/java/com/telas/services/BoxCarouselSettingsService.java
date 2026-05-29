package com.telas.services;

import com.telas.dtos.request.UpdateBoxCarouselSettingsRequestDto;
import com.telas.dtos.response.BoxPlayerSettingsResponseDto;

public interface BoxCarouselSettingsService {

    BoxPlayerSettingsResponseDto getSettings();

    BoxPlayerSettingsResponseDto updateSettings(UpdateBoxCarouselSettingsRequestDto request);
}
