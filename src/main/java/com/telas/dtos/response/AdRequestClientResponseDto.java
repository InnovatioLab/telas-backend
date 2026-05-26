package com.telas.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.telas.dtos.request.BusinessQuestionnaireAnswersRequestDto;
import com.telas.entities.AdRequest;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import com.telas.shared.utils.ValidateDataUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
public final class AdRequestClientResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5288515525105234502L;

    private final UUID id;

    @JsonProperty("active")
    private final boolean isActive;

    private final List<UUID> attachmentsIds;

    private final BusinessQuestionnaireAnswersRequestDto businessAnswers;

    private final Integer businessQuestionnaireVersion;

    private final Instant businessQuestionnaireUpdatedAt;

    public AdRequestClientResponseDto(
            AdRequest adRequest,
            BusinessQuestionnaireAnswersRequestDto businessAnswers,
            Integer businessQuestionnaireVersion,
            Instant businessQuestionnaireUpdatedAt) {
        id = adRequest.getId();
        isActive = adRequest.isActive();
        attachmentsIds = ValidateDataUtils.parseCsvUuids(adRequest.getAttachmentIds());
        this.businessAnswers = businessAnswers;
        this.businessQuestionnaireVersion = businessQuestionnaireVersion;
        this.businessQuestionnaireUpdatedAt = businessQuestionnaireUpdatedAt;
    }
}
