package com.telas.services;

import com.telas.dtos.request.BusinessQuestionnaireAnswersRequestDto;
import com.telas.entities.AdRequest;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface BusinessQuestionnaireService {

    Optional<BusinessQuestionnaireAnswersRequestDto> getDraftAnswers(UUID clientId);

    void saveDraft(UUID clientId, BusinessQuestionnaireAnswersRequestDto answers);

    void createQuestionnaireForNewAdRequest(UUID clientId, AdRequest adRequest, BusinessQuestionnaireAnswersRequestDto answers);

    void updateQuestionnaireForAdRequest(UUID clientId, UUID adRequestId, BusinessQuestionnaireAnswersRequestDto answers);

    byte[] exportTxtForAdRequest(UUID adRequestId, UUID actorClientId, boolean actorIsPrivileged);

    String resolveExportFileNameForAdRequest(UUID adRequestId, UUID actorClientId, boolean actorIsPrivileged);

    Optional<Integer> findLatestVersionByAdRequestId(UUID adRequestId);

    Optional<Instant> findLatestRevisionCreatedAt(UUID adRequestId);

    Map<UUID, QuestionnaireLatestMeta> findLatestMetadataByAdRequestIds(Collection<UUID> adRequestIds);

    Optional<BusinessQuestionnaireAnswersRequestDto> getLatestAnswersForClientAdRequest(UUID clientId, UUID adRequestId);
}
