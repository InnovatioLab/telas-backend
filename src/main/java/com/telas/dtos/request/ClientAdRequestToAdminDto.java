package com.telas.dtos.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClientAdRequestToAdminDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 2132868975487514316L;

    @NotEmpty
    private List<UUID> attachmentIds = new ArrayList<>();

    @NotNull
    @Valid
    private BusinessQuestionnaireAnswersRequestDto businessAnswers;
}
