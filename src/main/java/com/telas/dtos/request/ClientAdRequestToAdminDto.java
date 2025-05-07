package com.telas.dtos.request;


import com.telas.shared.constants.valitation.AdValidationMessages;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdRequestDto extends AttachmentRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 4107068995329945486L;

    @NotNull(message = AdValidationMessages.ID_REQUIRED)
    @NotEmpty(message = AdValidationMessages.ID_REQUIRED)
    List<UUID> adIds;
}