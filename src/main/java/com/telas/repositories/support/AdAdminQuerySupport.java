package com.telas.repositories.support;

import com.telas.dtos.response.AdminAdOperationRowDto;
import com.telas.enums.AdValidationType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class AdAdminQuerySupport {

    private static final String ADMIN_PLACEMENT_SELECT = """
            SELECT new com.telas.dtos.response.AdminAdOperationRowDto(
                ad.id, ad.name, ad.validation, ad.createdAt,
                advertiser.id, advertiser.businessName,
                partner.id, partner.businessName,
                COALESCE(addr.street, ''), COALESCE(addr.city, ''), COALESCE(addr.state, ''), COALESCE(addr.zipCode, ''),
                mon.id, ba.ip, sub.endsAt, sub.status
            )
            """;

    private static final String ADMIN_PLACEMENT_FROM = """
            FROM Ad ad
            JOIN ad.client advertiser
            LEFT JOIN ad.monitorAds ma
            LEFT JOIN ma.id.monitor mon
            LEFT JOIN mon.address addr
            LEFT JOIN addr.client partner
            LEFT JOIN mon.box box
            LEFT JOIN box.boxAddress ba
            LEFT JOIN Subscription sub ON sub.client.id = advertiser.id
             AND sub.status = com.telas.enums.SubscriptionStatus.ACTIVE
             AND mon IS NOT NULL
             AND EXISTS (
               SELECT 1 FROM SubscriptionMonitor smx
               WHERE smx.id.subscription.id = sub.id AND smx.id.monitor.id = mon.id
             )
            WHERE ad.validation = com.telas.enums.AdValidationType.APPROVED
            """;

    private static final String ADMIN_PLACEMENT_FILTERS = """
            AND (
                COALESCE(TRIM(:genericFilter), '') = ''
                OR LOWER(ad.name) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                OR LOWER(advertiser.businessName) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                OR LOWER(partner.businessName) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                OR LOWER(CONCAT(addr.street, addr.city, addr.state, addr.zipCode)) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
            )
            AND (COALESCE(TRIM(:advertiserNameFilter), '') = ''
                OR LOWER(advertiser.businessName) LIKE LOWER(CONCAT('%', TRIM(:advertiserNameFilter), '%')))
            AND (COALESCE(TRIM(:partnerNameFilter), '') = ''
                OR (partner IS NOT NULL AND LOWER(partner.businessName) LIKE LOWER(CONCAT('%', TRIM(:partnerNameFilter), '%'))))
            AND (COALESCE(TRIM(:boxIpFilter), '') = ''
                OR (ba IS NOT NULL AND LOWER(ba.ip) LIKE LOWER(CONCAT('%', TRIM(:boxIpFilter), '%'))))
            AND (COALESCE(TRIM(:screenContainsFilter), '') = ''
                OR LOWER(CONCAT(COALESCE(addr.street, ''), COALESCE(addr.city, ''), COALESCE(addr.state, ''), COALESCE(addr.zipCode, ''))) LIKE LOWER(CONCAT('%', TRIM(:screenContainsFilter), '%')))
            AND ad.createdAt >= :submissionDateFrom
            AND ad.createdAt <= :submissionDateTo
            """;

    private static final String MONITOR_AD_ADMIN_SELECT = """
            SELECT new com.telas.dtos.response.AdminAdOperationRowDto(
             ad.id, ad.name, ad.validation, ad.createdAt,
             advertiser.id, advertiser.businessName,
             partner.id, partner.businessName,
             addr.street, addr.city, addr.state, addr.zipCode,
             mon.id, ba.ip, sub.endsAt, sub.status
            )
            """;

    private static final String MONITOR_AD_ADMIN_FROM = """
            FROM MonitorAd ma
            JOIN ma.id.ad ad
            JOIN ad.client advertiser
            JOIN ma.id.monitor mon
            JOIN mon.address addr
            JOIN addr.client partner
            LEFT JOIN mon.box box
            LEFT JOIN box.boxAddress ba
            LEFT JOIN Subscription sub ON sub.client.id = advertiser.id
             AND sub.status = com.telas.enums.SubscriptionStatus.ACTIVE
             AND EXISTS (
               SELECT 1 FROM SubscriptionMonitor smx
               WHERE smx.id.subscription.id = sub.id AND smx.id.monitor.id = mon.id
             )
            WHERE (COALESCE(:genericFilter, '') = ''
             OR LOWER(ad.name) LIKE LOWER(CONCAT('%', :genericFilter, '%'))
             OR LOWER(advertiser.businessName) LIKE LOWER(CONCAT('%', :genericFilter, '%'))
             OR LOWER(partner.businessName) LIKE LOWER(CONCAT('%', :genericFilter, '%'))
             OR LOWER(CONCAT(addr.street, addr.city, addr.state, addr.zipCode)) LIKE LOWER(CONCAT('%', :genericFilter, '%'))
            )
            AND (:validation IS NULL OR ad.validation = :validation)
            """;

    @PersistenceContext
    private EntityManager entityManager;

    public Page<AdminAdOperationRowDto> searchAdsAdminOperationsWithoutPlacement(
            AdValidationType validation,
            String genericFilter,
            Pageable pageable) {
        String countJpql = """
                SELECT COUNT(ad)
                FROM Ad ad
                JOIN ad.client client
                WHERE ad.validation = :validation
                AND (
                    COALESCE(TRIM(:genericFilter), '') = ''
                    OR LOWER(client.businessName) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                    OR LOWER(ad.name) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                )
                """;
        String dataJpql = """
                SELECT new com.telas.dtos.response.AdminAdOperationRowDto(
                    ad.id, ad.name, ad.validation, ad.createdAt, client.id, client.businessName
                )
                FROM Ad ad
                JOIN ad.client client
                WHERE ad.validation = :validation
                AND (
                    COALESCE(TRIM(:genericFilter), '') = ''
                    OR LOWER(client.businessName) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                    OR LOWER(ad.name) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                )
                """;
        return paginate(dataJpql, countJpql, pageable, query -> query
                .setParameter("validation", validation)
                .setParameter("genericFilter", genericFilter));
    }

    public Page<AdminAdOperationRowDto> searchApprovedAdsAdminOperations(
            String genericFilter,
            String advertiserNameFilter,
            String partnerNameFilter,
            String boxIpFilter,
            String screenContainsFilter,
            Instant submissionDateFrom,
            Instant submissionDateTo,
            Pageable pageable) {
        String countJpql = "SELECT COUNT(DISTINCT ad.id) " + ADMIN_PLACEMENT_FROM + ADMIN_PLACEMENT_FILTERS;
        String dataJpql = "SELECT DISTINCT " + ADMIN_PLACEMENT_SELECT.substring("SELECT ".length())
                + ADMIN_PLACEMENT_FROM + ADMIN_PLACEMENT_FILTERS;
        return paginate(dataJpql, countJpql, pageable, query -> applyApprovedAdminFilters(
                query, genericFilter, advertiserNameFilter, partnerNameFilter,
                boxIpFilter, screenContainsFilter, submissionDateFrom, submissionDateTo));
    }

    public Page<AdminAdOperationRowDto> searchAdsAwaitingBoxDispatch(
            String genericFilter,
            String partnerNameFilter,
            String screenContainsFilter,
            Pageable pageable) {
        String countJpql = """
                SELECT COUNT(DISTINCT ad.id)
                FROM Ad ad
                JOIN ad.client advertiser
                LEFT JOIN ad.adRequest ar
                LEFT JOIN ar.targetMonitor monAr
                LEFT JOIN monAr.address addrAr
                LEFT JOIN addrAr.client screenOwnerAr
                LEFT JOIN monAr.box boxAr
                LEFT JOIN boxAr.boxAddress baAr
                LEFT JOIN ad.monitorAds ma
                LEFT JOIN ma.id.monitor monMa
                LEFT JOIN monMa.address addrMa
                LEFT JOIN addrMa.client screenOwnerMa
                LEFT JOIN monMa.box boxMa
                LEFT JOIN boxMa.boxAddress baMa
                WHERE ad.validation = com.telas.enums.AdValidationType.APPROVED
                AND advertiser.role = com.telas.enums.Role.PARTNER
                AND ad.partnerBoxStagedAt IS NULL
                AND ad.onAirNotifiedAt IS NULL
                AND (
                    (monAr IS NOT NULL AND screenOwnerAr IS NOT NULL AND screenOwnerAr.id <> advertiser.id
                     AND boxAr.active = true AND COALESCE(TRIM(baAr.ip), '') <> '')
                    OR
                    (monMa IS NOT NULL AND screenOwnerMa IS NOT NULL AND screenOwnerMa.id <> advertiser.id
                     AND boxMa.active = true AND COALESCE(TRIM(baMa.ip), '') <> '')
                )
                AND (
                    COALESCE(TRIM(:genericFilter), '') = ''
                    OR LOWER(ad.name) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                    OR LOWER(advertiser.businessName) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                    OR LOWER(COALESCE(screenOwnerAr.businessName, screenOwnerMa.businessName, '')) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                    OR LOWER(CONCAT(
                        COALESCE(addrAr.street, addrMa.street, ''),
                        COALESCE(addrAr.city, addrMa.city, ''),
                        COALESCE(addrAr.state, addrMa.state, ''),
                        COALESCE(addrAr.zipCode, addrMa.zipCode, '')
                    )) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                )
                AND (COALESCE(TRIM(:partnerNameFilter), '') = ''
                    OR LOWER(advertiser.businessName) LIKE LOWER(CONCAT('%', TRIM(:partnerNameFilter), '%')))
                AND (COALESCE(TRIM(:screenContainsFilter), '') = ''
                    OR LOWER(CONCAT(
                        COALESCE(addrAr.street, addrMa.street, ''),
                        COALESCE(addrAr.city, addrMa.city, ''),
                        COALESCE(addrAr.state, addrMa.state, ''),
                        COALESCE(addrAr.zipCode, addrMa.zipCode, '')
                    )) LIKE LOWER(CONCAT('%', TRIM(:screenContainsFilter), '%')))
                """;
        String dataJpql = """
                SELECT new com.telas.dtos.response.AdminAdOperationRowDto(
                    ad.id, ad.name, ad.validation, ad.createdAt,
                    advertiser.id, advertiser.businessName,
                    advertiser.id, advertiser.businessName,
                    COALESCE(addrAr.street, addrMa.street, ''),
                    COALESCE(addrAr.city, addrMa.city, ''),
                    COALESCE(addrAr.state, addrMa.state, ''),
                    COALESCE(addrAr.zipCode, addrMa.zipCode, ''),
                    COALESCE(monAr.id, monMa.id),
                    COALESCE(baAr.ip, baMa.ip),
                    null, null
                )
                FROM Ad ad
                JOIN ad.client advertiser
                LEFT JOIN ad.adRequest ar
                LEFT JOIN ar.targetMonitor monAr
                LEFT JOIN monAr.address addrAr
                LEFT JOIN addrAr.client screenOwnerAr
                LEFT JOIN monAr.box boxAr
                LEFT JOIN boxAr.boxAddress baAr
                LEFT JOIN ad.monitorAds ma
                LEFT JOIN ma.id.monitor monMa
                LEFT JOIN monMa.address addrMa
                LEFT JOIN addrMa.client screenOwnerMa
                LEFT JOIN monMa.box boxMa
                LEFT JOIN boxMa.boxAddress baMa
                WHERE ad.validation = com.telas.enums.AdValidationType.APPROVED
                AND advertiser.role = com.telas.enums.Role.PARTNER
                AND ad.partnerBoxStagedAt IS NULL
                AND ad.onAirNotifiedAt IS NULL
                AND (
                    (monAr IS NOT NULL AND screenOwnerAr IS NOT NULL AND screenOwnerAr.id <> advertiser.id
                     AND boxAr.active = true AND COALESCE(TRIM(baAr.ip), '') <> '')
                    OR
                    (monMa IS NOT NULL AND screenOwnerMa IS NOT NULL AND screenOwnerMa.id <> advertiser.id
                     AND boxMa.active = true AND COALESCE(TRIM(baMa.ip), '') <> '')
                )
                AND (
                    COALESCE(TRIM(:genericFilter), '') = ''
                    OR LOWER(ad.name) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                    OR LOWER(advertiser.businessName) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                    OR LOWER(COALESCE(screenOwnerAr.businessName, screenOwnerMa.businessName, '')) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                    OR LOWER(CONCAT(
                        COALESCE(addrAr.street, addrMa.street, ''),
                        COALESCE(addrAr.city, addrMa.city, ''),
                        COALESCE(addrAr.state, addrMa.state, ''),
                        COALESCE(addrAr.zipCode, addrMa.zipCode, '')
                    )) LIKE LOWER(CONCAT('%', TRIM(:genericFilter), '%'))
                )
                AND (COALESCE(TRIM(:partnerNameFilter), '') = ''
                    OR LOWER(advertiser.businessName) LIKE LOWER(CONCAT('%', TRIM(:partnerNameFilter), '%')))
                AND (COALESCE(TRIM(:screenContainsFilter), '') = ''
                    OR LOWER(CONCAT(
                        COALESCE(addrAr.street, addrMa.street, ''),
                        COALESCE(addrAr.city, addrMa.city, ''),
                        COALESCE(addrAr.state, addrMa.state, ''),
                        COALESCE(addrAr.zipCode, addrMa.zipCode, '')
                    )) LIKE LOWER(CONCAT('%', TRIM(:screenContainsFilter), '%')))
                """;
        return paginate(dataJpql, countJpql, pageable, query -> query
                .setParameter("genericFilter", genericFilter)
                .setParameter("partnerNameFilter", partnerNameFilter)
                .setParameter("screenContainsFilter", screenContainsFilter));
    }

    public Page<AdminAdOperationRowDto> searchMonitorAdAdminOperations(
            String genericFilter,
            AdValidationType validation,
            Pageable pageable) {
        String countJpql = "SELECT COUNT(ma) " + MONITOR_AD_ADMIN_FROM;
        String dataJpql = MONITOR_AD_ADMIN_SELECT + MONITOR_AD_ADMIN_FROM;
        return paginate(dataJpql, countJpql, pageable, query -> query
                .setParameter("genericFilter", genericFilter)
                .setParameter("validation", validation));
    }

    private jakarta.persistence.Query applyApprovedAdminFilters(
            jakarta.persistence.Query query,
            String genericFilter,
            String advertiserNameFilter,
            String partnerNameFilter,
            String boxIpFilter,
            String screenContainsFilter,
            Instant submissionDateFrom,
            Instant submissionDateTo) {
        return query
                .setParameter("genericFilter", genericFilter)
                .setParameter("advertiserNameFilter", advertiserNameFilter)
                .setParameter("partnerNameFilter", partnerNameFilter)
                .setParameter("boxIpFilter", boxIpFilter)
                .setParameter("screenContainsFilter", screenContainsFilter)
                .setParameter("submissionDateFrom", submissionDateFrom)
                .setParameter("submissionDateTo", submissionDateTo);
    }

    private Page<AdminAdOperationRowDto> paginate(
            String dataJpql,
            String countJpql,
            Pageable pageable,
            java.util.function.Function<jakarta.persistence.Query, jakarta.persistence.Query> parameterizer) {
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        parameterizer.apply(countQuery);
        long total = countQuery.getSingleResult();

        TypedQuery<AdminAdOperationRowDto> dataQuery = entityManager.createQuery(dataJpql, AdminAdOperationRowDto.class);
        parameterizer.apply(dataQuery);
        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());
        List<AdminAdOperationRowDto> content = dataQuery.getResultList();
        return new PageImpl<>(content, pageable, total);
    }
}
