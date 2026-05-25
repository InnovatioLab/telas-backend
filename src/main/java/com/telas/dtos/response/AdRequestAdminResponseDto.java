package com.telas.dtos.response;

import com.telas.entities.AdRequest;
import com.telas.enums.AdRequestOrigin;
import com.telas.enums.AdRequestWorkflowStatus;
import com.telas.enums.AdValidationType;
import com.telas.enums.PartnerSubmissionMode;
import com.telas.enums.Role;
import com.telas.helpers.AdRequestWorkflowResolver;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public final class AdRequestAdminResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5288515525105234502L;

    private final UUID id;

    private final UUID clientId;

    private final String clientName;

    private final Role clientRole;

    private final boolean isActive;

    private final LocalDate submissionDate;

    private final long waitingDays;

    private final List<RefusedAdResponseDto> refusedAds;

    private final List<LinkResponseDto> attachments;

    private final LinkResponseDto ad;

    private final Integer businessQuestionnaireVersion;

    private final Instant businessQuestionnaireUpdatedAt;

    private final AdRequestOrigin requestOrigin;

    private final PartnerSubmissionMode submissionMode;

    private final UUID targetMonitorId;

    private final String targetMonitorSummary;

    private final AdRequestWorkflowStatus workflowStatus;

    private final String adminActionLabel;

    private final AdValidationType adValidation;

    private final int attachmentCount;

    private final boolean hasAdMedia;

    private final boolean partnerRemovalRequested;

    public AdRequestAdminResponseDto(
            AdRequest adRequest,
            Integer businessQuestionnaireVersion,
            Instant businessQuestionnaireUpdatedAt,
            int attachmentCount,
            boolean partnerRemovalRequested) {
        id = adRequest.getId();
        clientId = adRequest.getClient().getId();
        clientName = adRequest.getClient().getBusinessName();
        clientRole = adRequest.getClient().getRole();
        isActive = adRequest.isActive();
        submissionDate = adRequest.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        waitingDays = ChronoUnit.DAYS.between(adRequest.getCreatedAt(), Instant.now());
        attachments = List.of();
        ad = null;
        this.businessQuestionnaireVersion = businessQuestionnaireVersion;
        this.businessQuestionnaireUpdatedAt = businessQuestionnaireUpdatedAt;
        this.requestOrigin = adRequest.getRequestOrigin();
        this.submissionMode = adRequest.getSubmissionMode();
        this.targetMonitorId = adRequest.getTargetMonitor() != null
                ? adRequest.getTargetMonitor().getId()
                : null;
        this.targetMonitorSummary = buildMonitorSummary(adRequest);
        this.workflowStatus = AdRequestWorkflowResolver.resolve(adRequest);
        this.adminActionLabel = AdRequestWorkflowResolver.adminActionLabel(this.workflowStatus);
        this.adValidation = adRequest.getAd() != null ? adRequest.getAd().getValidation() : null;
        this.attachmentCount = attachmentCount;
        this.hasAdMedia = adRequest.getAd() != null;
        this.partnerRemovalRequested = partnerRemovalRequested;

        refusedAds = adRequest.getAd() != null && !adRequest.getAd().getRefusedAds().isEmpty() ?
                adRequest.getAd().getRefusedAds().stream()
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .map(RefusedAdResponseDto::new)
                        .toList() : List.of();
    }

    public AdRequestAdminResponseDto(
            AdRequest adRequest,
            Map<String, Object> linkResponseData,
            Integer businessQuestionnaireVersion,
            Instant businessQuestionnaireUpdatedAt) {
        id = adRequest.getId();
        clientId = adRequest.getClient().getId();
        clientName = adRequest.getClient().getBusinessName();
        clientRole = adRequest.getClient().getRole();
        isActive = adRequest.isActive();
        submissionDate = adRequest.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        waitingDays = ChronoUnit.DAYS.between(adRequest.getCreatedAt(), Instant.now());
        attachments = (List<LinkResponseDto>) linkResponseData.get("attachments");
        ad = (LinkResponseDto) linkResponseData.get("ad");
        this.businessQuestionnaireVersion = businessQuestionnaireVersion;
        this.businessQuestionnaireUpdatedAt = businessQuestionnaireUpdatedAt;
        this.requestOrigin = adRequest.getRequestOrigin();
        this.submissionMode = adRequest.getSubmissionMode();
        this.targetMonitorId = adRequest.getTargetMonitor() != null
                ? adRequest.getTargetMonitor().getId()
                : null;
        this.targetMonitorSummary = buildMonitorSummary(adRequest);
        this.workflowStatus = AdRequestWorkflowResolver.resolve(adRequest);
        this.adminActionLabel = AdRequestWorkflowResolver.adminActionLabel(this.workflowStatus);
        this.adValidation = adRequest.getAd() != null ? adRequest.getAd().getValidation() : null;

        refusedAds = adRequest.getAd() != null && !adRequest.getAd().getRefusedAds().isEmpty() ?
                adRequest.getAd().getRefusedAds().stream()
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .map(RefusedAdResponseDto::new)
                        .toList() : List.of();
        this.attachmentCount = attachments != null ? attachments.size() : 0;
        this.hasAdMedia = ad != null;
        this.partnerRemovalRequested = adRequest.getAd() != null
                && adRequest.getAd().getPartnerRemovalRequestedAt() != null;
    }

    private static String buildMonitorSummary(AdRequest adRequest) {
        if (adRequest.getTargetMonitor() == null || adRequest.getTargetMonitor().getAddress() == null) {
            return null;
        }
        return adRequest.getTargetMonitor().getAddress().resolveMapLocationName();
    }
}
