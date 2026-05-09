package com.telas.dtos.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.dtos.validation.ValidUrl;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentReferenceMetaDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private UUID attachmentId;

    @Size(max = 50)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String slogan;

    @ValidUrl(message = "Invalid Brand Guideline URL format")
    @Size(max = 255)
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String brandGuidelineUrl;
}
