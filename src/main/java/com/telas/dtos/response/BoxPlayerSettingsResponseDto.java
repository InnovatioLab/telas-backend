package com.telas.dtos.response;

import com.telas.entities.BoxCarouselSettings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoxPlayerSettingsResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int displayDurationMs;
    private int transitionDurationMs;
    private int totalSlots;

    public static BoxPlayerSettingsResponseDto fromEntity(BoxCarouselSettings entity) {
        return new BoxPlayerSettingsResponseDto(
                entity.getDisplayDurationMs(),
                entity.getTransitionDurationMs(),
                entity.getTotalSlots());
    }
}
