package com.telas.services.impl;

import com.telas.dtos.request.filters.AdminAdOperationsFilterRequestDto;
import com.telas.dtos.response.AdminAdOperationRowDto;
import com.telas.dtos.response.AdminExpiryNotificationDto;
import com.telas.dtos.response.PaginationResponseDto;
import com.telas.entities.Ad;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.entities.Notification;
import com.telas.entities.Subscription;
import com.telas.enums.NotificationReference;
import com.telas.enums.SubscriptionStatus;
import com.telas.helpers.MonitorHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.enums.AdValidationType;
import com.telas.repositories.AdRepository;
import com.telas.repositories.MonitorAdRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.repositories.NotificationRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.helpers.AttachmentHelper;
import com.telas.services.AdminAdOperationsService;
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.utils.PaginationFilterUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAdOperationsServiceImpl implements AdminAdOperationsService {

    private static final Set<NotificationReference> EXPIRY_NOTIFICATION_REFERENCES = Set.of(
            NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_REMINDER,
            NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_10_DAYS,
            NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_5_DAYS,
            NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_3_DAYS,
            NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_PENULTIMATE_DAY
    );

    private static final Map<NotificationReference, String> EXPIRY_LABELS = buildExpiryLabels();

    private static Map<NotificationReference, String> buildExpiryLabels() {
        Map<NotificationReference, String> m = new EnumMap<>(NotificationReference.class);
        m.put(NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_REMINDER, "15 days before expiry (e-mail)");
        m.put(NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_10_DAYS, "10 days before expiry (e-mail)");
        m.put(NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_5_DAYS, "5 days before expiry (e-mail)");
        m.put(NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_3_DAYS, "3 days before expiry (e-mail)");
        m.put(NotificationReference.SUBSCRIPTION_ABOUT_TO_EXPIRY_PENULTIMATE_DAY, "Final reminder before end (e-mail)");
        return m;
    }

    private static final DateTimeFormatter CSV_INSTANT = DateTimeFormatter.ISO_INSTANT;

    private final AdRepository adRepository;

    private final MonitorAdRepository monitorAdRepository;
    private final MonitorRepository monitorRepository;
    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AttachmentHelper attachmentHelper;
    private final MonitorHelper monitorHelper;
    private final UnusedSingleAdDeletionService unusedSingleAdDeletionService;

    private static String trimOrEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private void enrichRowsWithAdLinks(List<AdminAdOperationRowDto> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<UUID> ids = rows.stream()
                .map(AdminAdOperationRowDto::getAdId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<UUID, Ad> byId = adRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Ad::getId, a -> a));
        for (AdminAdOperationRowDto row : rows) {
            Ad ad = byId.get(row.getAdId());
            if (ad != null) {
                row.setAdLink(attachmentHelper.getStringLinkFromAd(ad));
                row.setAdMediaType(ad.getType());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<AdminAdOperationRowDto>> findPage(AdminAdOperationsFilterRequestDto request) {
        authenticatedUserService.validateAdminOrAdsManageAccess();
        String gf = request.getGenericFilter() == null ? "" : request.getGenericFilter().trim();
        AdValidationType validation = request.getValidation();
        Sort sort = request.resolveSort(validation);
        Pageable pageable = PaginationFilterUtil.getPageable(request, sort);
        Page<AdminAdOperationRowDto> page;
        if (validation == AdValidationType.APPROVED) {
            page = adRepository.searchApprovedAdsAdminOperations(
                    gf,
                    trimOrEmpty(request.getAdvertiserName()),
                    trimOrEmpty(request.getPartnerName()),
                    trimOrEmpty(request.getBoxIp()),
                    trimOrEmpty(request.getScreenContains()),
                    request.effectiveSubmissionDateFrom(),
                    request.effectiveSubmissionDateTo(),
                    pageable
            );
        } else if (validation == AdValidationType.PENDING || validation == AdValidationType.REJECTED) {
            page = adRepository.searchAdsAdminOperationsWithoutPlacement(validation, gf, pageable);
        } else {
            page = monitorAdRepository.searchAdminOperations(
                    gf,
                    validation,
                    pageable
            );
        }
        enrichRowsWithAdLinks(page.getContent());
        return PaginationResponseDto.fromResult(
                page.getContent(),
                (int) page.getTotalElements(),
                page.getTotalPages(),
                request.getPage()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminExpiryNotificationDto> listExpiryNotifications(UUID advertiserClientId) {
        authenticatedUserService.validateAdminOrAdsManageAccess();
        List<Notification> list = notificationRepository.findByClientIdAndReferenceInOrderByCreatedAtDesc(
                advertiserClientId,
                EXPIRY_NOTIFICATION_REFERENCES
        );
        return list.stream()
                .map(n -> new AdminExpiryNotificationDto(
                        n.getReference().name(),
                        n.getCreatedAt(),
                        EXPIRY_LABELS.getOrDefault(n.getReference(), n.getReference().name())
                ))
                .toList();
    }

    @Override
    @Transactional
    public void deleteApprovedAd(UUID adId) {
        authenticatedUserService.validateAdminOrAdsManageAccess();
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_NOT_FOUND));
        if (!AdValidationType.APPROVED.equals(ad.getValidation())) {
            throw new BusinessRuleException(AdValidationMessages.AD_MUST_BE_APPROVED_TO_DELETE);
        }
        String adName = ad.getName();
        List<MonitorAd> placements = monitorAdRepository.findByAdIdWithMonitor(adId);
        Map<UUID, Monitor> monitorsById = new LinkedHashMap<>();
        for (MonitorAd placement : placements) {
            Monitor monitor = placement.getMonitor();
            if (monitor != null) {
                monitorsById.putIfAbsent(monitor.getId(), monitor);
            }
        }
        for (Monitor monitor : monitorsById.values()) {
            monitor.getMonitorAds().removeIf(ma ->
                    ma.getAd() != null && adId.equals(ma.getAd().getId()));
            monitorRepository.save(monitor);
            if (monitor.isAbleToSendBoxRequest()) {
                monitorHelper.sendBoxesMonitorsRemoveAds(monitor, List.of(adName));
            }
        }
        unusedSingleAdDeletionService.deleteAdInNewTransaction(adId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSubscriptionsCsv() {
        authenticatedUserService.validateAdmin();
        List<Subscription> rows = subscriptionRepository.findByStatusInForExport(
                List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING, SubscriptionStatus.EXPIRED)
        );
        StringBuilder sb = new StringBuilder();
        sb.append("businessName,email,subscriptionId,status,recurrence,endsAt,totalPaid,bonus\n");
        for (Subscription s : rows) {
            if (s.getClient() == null || s.getClient().getContact() == null) {
                continue;
            }
            BigDecimal paid = s.getPaidAmount();
            String ends = s.getEndsAt() == null ? "" : CSV_INSTANT.format(s.getEndsAt().atOffset(ZoneOffset.UTC));
            sb.append(csvEscape(s.getClient().getBusinessName())).append(',')
                    .append(csvEscape(s.getClient().getContact().getEmail())).append(',')
                    .append(s.getId()).append(',')
                    .append(s.getStatus()).append(',')
                    .append(s.getRecurrence()).append(',')
                    .append(ends).append(',')
                    .append(paid != null ? paid.toPlainString() : "0").append(',')
                    .append(s.isBonus() ? "yes" : "no")
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csvEscape(String v) {
        if (v == null) {
            return "";
        }
        String x = v.replace("\"", "\"\"");
        if (x.contains(",") || x.contains("\"") || x.contains("\n")) {
            return "\"" + x + "\"";
        }
        return x;
    }
}
