package com.telas.services;

import com.telas.dtos.request.AdMessageRequestDto;
import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.BusinessQuestionnaireAnswersRequestDto;
import com.telas.dtos.request.ClientAdRequestToAdminDto;
import com.telas.dtos.request.RefusedAdRequestDto;
import com.telas.dtos.response.AdMessageResponseDto;
import com.telas.dtos.response.AdminClientMessageRowDto;
import com.telas.enums.AdValidationType;
import com.telas.shared.model.NamedDownloadResource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientAdRequestService {

    void requestAdCreation(ClientAdRequestToAdminDto request);

    Optional<BusinessQuestionnaireAnswersRequestDto> getBusinessQuestionnaireDraft();

    void saveBusinessQuestionnaireDraft(BusinessQuestionnaireAnswersRequestDto answers);

    void updateAdRequestBusinessQuestionnaire(UUID adRequestId, BusinessQuestionnaireAnswersRequestDto answers);

    NamedDownloadResource exportAdRequestBusinessQuestionnaireTxtAdmin(UUID adRequestId);

    void uploadAds(AttachmentRequestDto request, UUID clientId);

    void uploadAdsForAdRequest(AttachmentRequestDto request, UUID adRequestId);

    void approveAdRequestToAds(UUID adRequestId);

    void cancelAdRequest(UUID adRequestId);

    void validateAd(UUID adId, AdValidationType validation, RefusedAdRequestDto request);

    List<AdMessageResponseDto> listAdMessages(UUID adId);

    void sendAdMessage(UUID adId, AdMessageRequestDto request);

    List<AdminClientMessageRowDto> listClientMessagesHistory(UUID clientId);
}
