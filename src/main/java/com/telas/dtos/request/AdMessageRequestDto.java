package com.telas.dtos.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdMessageRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 3331782582984444746L;

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must have at most 1000 characters")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String message;
}

