package com.telas.dtos.request;

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
}
