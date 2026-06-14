package com.telas.services.impl;

import com.telas.dtos.response.AdFlowSummaryDto;
import com.telas.dtos.response.FlowEventDto;
import com.telas.entities.Ad;
import com.telas.entities.AdMessage;
import com.telas.entities.AdRequest;
import com.telas.entities.RefusedAd;
import com.telas.enums.AdValidationType;
import com.telas.helpers.AdMediaLinkFactory;
import com.telas.monitoring.entities.ApplicationLogEntity;
import com.telas.monitoring.repositories.ApplicationLogEntityRepository;
import com.telas.repositories.AdMessageRepository;
import com.telas.repositories.AdRequestRepository;
import com.telas.services.AdminAdFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAdFlowServiceImpl implements AdminAdFlowService {

    private final AdRequestRepository adRequestRepository;
    private final AdMessageRepository adMessageRepository;
    private final AdMediaLinkFactory adMediaLinkFactory;
    private final ApplicationLogEntityRepository applicationLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdFlowSummaryDto> listFlows(UUID clientId, Pageable pageable) {
        Page<AdRequest> page = adRequestRepository.findForFlowView(clientId, pageable);
        List<AdRequest> content = page.getContent();

        // batch-fetch messages for all ads in this page to avoid N+1
        List<UUID> adIds = content.stream()
                .map(AdRequest::getAd)
                .filter(ad -> ad != null)
                .map(Ad::getId)
                .collect(Collectors.toList());

        Map<UUID, List<AdMessage>> messagesByAdId = (adIds.isEmpty()
                ? List.<AdMessage>of()
                : adMessageRepository.findAllByAdIdInOrderByCreatedAtAsc(adIds))
                .stream()
                .collect(Collectors.groupingBy(m -> m.getAd().getId()));

        Map<UUID, List<ApplicationLogEntity>> errorLogsByAdRequestId =
                fetchErrorLogsByFlow(content, adIds);

        return page.map(ar -> toFlowSummary(ar, messagesByAdId, errorLogsByAdRequestId));
    }

    private Map<UUID, List<ApplicationLogEntity>> fetchErrorLogsByFlow(
            List<AdRequest> requests, List<UUID> adIds) {
        Map<String, UUID> adIdToAdRequestId = new HashMap<>();
        Set<String> adRequestIdSet = new HashSet<>();

        for (AdRequest ar : requests) {
            adRequestIdSet.add(ar.getId().toString());
            if (ar.getAd() != null) {
                adIdToAdRequestId.put(ar.getAd().getId().toString(), ar.getId());
            }
        }

        Set<String> allIds = new HashSet<>();
        allIds.addAll(adRequestIdSet);
        allIds.addAll(adIdToAdRequestId.keySet());

        if (allIds.isEmpty()) return Map.of();

        String commaSeparatedIds = String.join(",", allIds);
        String uuidPattern = String.join("|", allIds);

        List<ApplicationLogEntity> logs;
        try {
            logs = applicationLogRepository.findByAdFlowIds(commaSeparatedIds, uuidPattern);
        } catch (Exception ignored) {
            return Map.of();
        }

        Map<UUID, List<ApplicationLogEntity>> result = new HashMap<>();
        for (ApplicationLogEntity log : logs) {
            UUID adRequestId = resolveAdRequestId(log, adRequestIdSet, adIdToAdRequestId);
            if (adRequestId != null) {
                result.computeIfAbsent(adRequestId, k -> new ArrayList<>()).add(log);
            }
        }
        return result;
    }

    private UUID resolveAdRequestId(ApplicationLogEntity log,
            Set<String> adRequestIdSet, Map<String, UUID> adIdToAdRequestId) {
        Map<String, Object> meta = log.getMetadataJson();
        if (meta != null) {
            Object arId = meta.get("adRequestId");
            if (arId != null && adRequestIdSet.contains(arId.toString())) {
                try { return UUID.fromString(arId.toString()); } catch (Exception ignored) {}
            }
            Object aId = meta.get("adId");
            if (aId != null) {
                UUID mapped = adIdToAdRequestId.get(aId.toString());
                if (mapped != null) return mapped;
            }
        }
        String endpoint = log.getEndpoint();
        if (endpoint != null) {
            for (String id : adRequestIdSet) {
                if (endpoint.contains(id)) {
                    try { return UUID.fromString(id); } catch (Exception ignored) {}
                }
            }
            for (Map.Entry<String, UUID> entry : adIdToAdRequestId.entrySet()) {
                if (endpoint.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private AdFlowSummaryDto toFlowSummary(AdRequest ar, Map<UUID, List<AdMessage>> messagesByAdId,
            Map<UUID, List<ApplicationLogEntity>> errorLogsByAdRequestId) {
        Ad ad = ar.getAd();
        List<FlowEventDto> events = new ArrayList<>();
        String clientName = ar.getClient() != null ? ar.getClient().getBusinessName() : null;

        // 1. submission
        if (ar.getCreatedAt() != null) {
            String actor = ar.getUsernameCreate() != null ? ar.getUsernameCreate() : clientName;
            events.add(new FlowEventDto(
                    ar.getCreatedAt(), "SUBMITTED", "CLIENT", actor,
                    "Ad request submitted", null));
        }

        if (ad != null) {
            // 2. admin validated / rejected
            String adminActor = ad.getUsernameCreate();
            if (ad.getValidation() == AdValidationType.APPROVED && ad.getCreatedAt() != null) {
                events.add(new FlowEventDto(
                        ad.getCreatedAt(), "VALIDATED", "ADMIN", adminActor,
                        "Ad approved by admin", null));
            } else if (ad.getValidation() == AdValidationType.REJECTED && ad.getCreatedAt() != null) {
                events.add(new FlowEventDto(
                        ad.getCreatedAt(), "REJECTED_ADMIN", "ADMIN", adminActor,
                        "Ad rejected by admin", null));
            } else if (ad.getCreatedAt() != null) {
                events.add(new FlowEventDto(
                        ad.getCreatedAt(), "PENDING_VALIDATION", "SYSTEM", null,
                        "Ad awaiting admin validation", null));
            }

            // 3. refusals (client-side rejections)
            for (RefusedAd refusal : ad.getRefusedAds()) {
                if (refusal.getCreatedAt() != null) {
                    String detail = refusal.getJustification();
                    if (refusal.getDescription() != null && !refusal.getDescription().isBlank()) {
                        detail = detail + " — " + refusal.getDescription();
                    }
                    String refuseActor = refusal.getUsernameCreate() != null ? refusal.getUsernameCreate() : clientName;
                    events.add(new FlowEventDto(
                            refusal.getCreatedAt(), "REFUSED", "CLIENT", refuseActor,
                            "Client refused ad", detail));
                }
            }

            // 4. messages
            List<AdMessage> messages = messagesByAdId.getOrDefault(ad.getId(), List.of());
            for (AdMessage msg : messages) {
                if (msg.getCreatedAt() != null) {
                    String actorType = msg.getSenderRole() != null ? msg.getSenderRole().name() : "SYSTEM";
                    String msgActor = msg.getUsernameCreate();
                    String preview = msg.getMessage();
                    if (preview != null && preview.length() > 120) {
                        preview = preview.substring(0, 120) + "…";
                    }
                    events.add(new FlowEventDto(
                            msg.getCreatedAt(), "MESSAGE", actorType, msgActor,
                            "Message sent", preview));
                }
            }

            // 5. on-air
            if (ad.getOnAirNotifiedAt() != null) {
                events.add(new FlowEventDto(
                        ad.getOnAirNotifiedAt(), "ON_AIR", "SYSTEM", null,
                        "Ad on air — client notified", null));
            }

            // 6. staged to partner box
            if (ad.getPartnerBoxStagedAt() != null) {
                events.add(new FlowEventDto(
                        ad.getPartnerBoxStagedAt(), "STAGED", "SYSTEM", null,
                        "Staged to partner box", null));
            }

            // 7. partner removal requested
            if (ad.getPartnerRemovalRequestedAt() != null) {
                String partnerActor = ad.getUsernameUpdate();
                events.add(new FlowEventDto(
                        ad.getPartnerRemovalRequestedAt(), "REMOVAL_REQUESTED", "PARTNER", partnerActor,
                        "Partner requested removal", ad.getPartnerRemovalMessage()));
            }

            // 8. deletion scheduled
            if (ad.getDeletionScheduledAt() != null) {
                events.add(new FlowEventDto(
                        ad.getDeletionScheduledAt(), "DELETION_SCHEDULED", "SYSTEM", null,
                        "Removal scheduled", null));
            }
        }

        // error / validation events from application logs
        List<ApplicationLogEntity> errorLogs = errorLogsByAdRequestId.getOrDefault(ar.getId(), List.of());
        for (ApplicationLogEntity log : errorLogs) {
            if (log.getCreatedAt() != null) {
                String eventType = "ERROR".equals(log.getLevel()) ? "ERROR" : "VALIDATION_ERROR";
                String label = log.getMessage() != null
                        ? (log.getMessage().length() > 150 ? log.getMessage().substring(0, 150) + "…" : log.getMessage())
                        : "System error";
                String detail = null;
                if (log.getStackTrace() != null && !log.getStackTrace().isBlank()) {
                    String firstLine = log.getStackTrace().split("\n")[0].trim();
                    detail = firstLine.length() > 200 ? firstLine.substring(0, 200) + "…" : firstLine;
                }
                events.add(new FlowEventDto(log.getCreatedAt(), eventType, "SYSTEM", null, label, detail));
            }
        }

        events.sort(Comparator.comparing(FlowEventDto::occurredAt));

        Instant lastEventAt = events.isEmpty() ? ar.getCreatedAt()
                : events.get(events.size() - 1).occurredAt();

        String currentStatus = deriveStatus(ar, ad);
        String adTitle = ad != null ? ad.getName() : null;
        String adMimeType = ad != null ? ad.getType() : null;
        String adPreviewUrl = null;
        if (ad != null) {
            try { adPreviewUrl = adMediaLinkFactory.getLink(ad); } catch (Exception ignored) {}
        }
        String clientDisplayName = clientName != null ? clientName : "—";

        return new AdFlowSummaryDto(ar.getId(), clientDisplayName, adTitle, adMimeType, adPreviewUrl, currentStatus, lastEventAt, events);
    }

    private String deriveStatus(AdRequest ar, Ad ad) {
        if (!ar.isActive()) return "INACTIVE";
        if (ad == null) return "PENDING_AD";
        return switch (ad.getValidation()) {
            case PENDING -> "PENDING_VALIDATION";
            case APPROVED -> ad.getOnAirNotifiedAt() != null ? "ON_AIR" : "APPROVED";
            case REJECTED -> "REJECTED";
        };
    }
}
