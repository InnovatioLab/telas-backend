package com.telas.dtos.request;

import com.telas.dtos.response.BoxPlayerSettingsResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoxPlaylistPushRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<UpdateBoxMonitorsAdRequestDto> ads;
    private BoxPlayerSettingsResponseDto playerSettings;
}
