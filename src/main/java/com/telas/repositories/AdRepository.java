package com.telas.repositories;

import com.telas.entities.Ad;
import com.telas.enums.AdValidationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdRepository extends JpaRepository<Ad, UUID>, JpaSpecificationExecutor<Ad> {
    @Query("""
                SELECT ad FROM Ad ad
                JOIN FETCH ad.client c
                LEFT JOIN FETCH c.subscriptions s
                LEFT JOIN FETCH s.subscriptionMonitors sm
                WHERE (
                    c.role <> 'ADMIN'
                    AND ad.validation = :validation
                    AND s.status = 'ACTIVE'
                    AND (s.endsAt IS NULL OR s.endsAt > CURRENT_TIMESTAMP)
                    AND sm.id.monitor.id = :monitorId
                )
                OR c.role = 'ADMIN'
            """)
    List<Ad> findAllValidAdsForMonitor(
            @Param("validation") AdValidationType validation,
            @Param("monitorId") UUID monitorId
    );

    @Query("""
            SELECT ad FROM Ad ad
            WHERE ad.validation = 'APPROVED'
              AND ad.id NOT IN (
                  SELECT ma.id.ad.id FROM MonitorAd ma WHERE ma.id.monitor.id = :monitorId
              )
              AND (
                  COALESCE(TRIM(:name), '') = ''
                  OR LOWER(ad.name) LIKE CONCAT('%', LOWER(TRIM(:name)), '%')
                  OR LOWER(ad.client.businessName) LIKE CONCAT('%', LOWER(TRIM(:name)), '%')
              )
            """)
    List<Ad> findAllApprovedNotInMonitorFiltered(@Param("monitorId") UUID monitorId, @Param("name") String name);

}