package com.telas.repositories;

import com.telas.dtos.response.AdminAdOperationRowDto;
import com.telas.entities.MonitorAd;
import com.telas.entities.MonitorAdPK;
import com.telas.enums.AdValidationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitorAdRepository extends JpaRepository<MonitorAd, MonitorAdPK> {

    @Query("SELECT COUNT(ma) FROM MonitorAd ma WHERE ma.id.ad.id = :adId")
    long countByAdId(@Param("adId") java.util.UUID adId);

    @Query(
            countQuery = """
                    SELECT COUNT(ma)
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
                    """,
            value = """
                    SELECT new com.telas.dtos.response.AdminAdOperationRowDto(
                     ad.id, ad.name, ad.validation,
                     advertiser.id, advertiser.businessName,
                     partner.id, partner.businessName,
                     addr.street, addr.city, addr.state, addr.zipCode,
                     mon.id, ba.ip, sub.endsAt, sub.status
                    )
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
                    ORDER BY ad.name ASC, mon.id ASC
                    """)
    Page<AdminAdOperationRowDto> searchAdminOperations(
            @Param("genericFilter") String genericFilter,
            @Param("validation") AdValidationType validation,
            Pageable pageable);
}