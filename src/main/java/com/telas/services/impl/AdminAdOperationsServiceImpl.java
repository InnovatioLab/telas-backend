package com.telas.services.impl;

import com.telas.dtos.request.filters.AdminAdOperationsFilterRequestDto;
import com.telas.dtos.response.AdminAdOperationRowDto;
import com.telas.dtos.response.AdminExpiryNotificationDto;
import com.telas.dtos.response.PaginationResponseDto;
import com.telas.entities.Notification;
import com.telas.entities.Subscription;
import com.telas.enums.NotificationReference;
import com.telas.enums.SubscriptionStatus;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.MonitorAdRepository;
import com.telas.repositories.NotificationRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.AdminAdOperationsService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    private final MonitorAdRepository monitorAdRepository;
    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<AdminAdOperationRowDto>> findPage(AdminAdOperationsFilterRequestDto request) {
        authenticatedUserService.validateAdmin();
        String gf = request.getGenericFilter() == null ? "" : request.getGenericFilter().trim();
        Pageable pageable = PaginationFilterUtil.getPageable(request, Sort.unsorted());
        Page<AdminAdOperationRowDto> page = monitorAdRepository.searchAdminOperations(
                gf,
                request.getValidation(),
                pageable
        );
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
        authenticatedUserService.validateAdmin();
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
