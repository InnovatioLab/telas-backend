package com.telas.repositories;

import com.telas.entities.MonitorAd;
import com.telas.entities.MonitorAdPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitorAdRepository extends JpaRepository<MonitorAd, MonitorAdPK> {

    interface CountByPartnerRow {
        java.util.UUID getPartnerId();
        long getAdsCount();
    }

    @Query("SELECT COUNT(ma) FROM MonitorAd ma WHERE ma.id.ad.id = :adId")
    long countByAdId(@Param("adId") java.util.UUID adId);

    @Query("""
            SELECT DISTINCT mon.id
            FROM MonitorAd ma
            JOIN ma.id.monitor mon
            JOIN ma.id.ad ad
            WHERE ad.client.id = :partnerId
            AND ad.validation = com.telas.enums.AdValidationType.APPROVED
            AND ad.onAirNotifiedAt IS NOT NULL
            """)
    java.util.List<java.util.UUID> findDistinctMonitorIdsByAdvertiserClientIdOnAir(
            @Param("partnerId") java.util.UUID partnerId);

    @Query("""
            SELECT DISTINCT mon.id
            FROM MonitorAd ma
            JOIN ma.id.monitor mon
            JOIN ma.id.ad ad
            WHERE ad.client.id = :partnerId
            AND ad.validation = com.telas.enums.AdValidationType.APPROVED
            """)
    java.util.List<java.util.UUID> findDistinctMonitorIdsByAdvertiserClientIdApproved(
            @Param("partnerId") java.util.UUID partnerId);

    @Query("""
            SELECT ma FROM MonitorAd ma
            JOIN FETCH ma.id.monitor mon
            LEFT JOIN FETCH mon.box box
            LEFT JOIN FETCH box.boxAddress
            WHERE ma.id.ad.id = :adId
            """)
    java.util.List<MonitorAd> findByAdIdWithMonitor(@Param("adId") java.util.UUID adId);

    @Modifying
    @Query("DELETE FROM MonitorAd ma WHERE ma.id.ad.id = :adId")
    void deleteByAdId(@Param("adId") java.util.UUID adId);

    @Query("""
            SELECT addr.client.id as partnerId, COUNT(ma) as adsCount
            FROM MonitorAd ma
            JOIN ma.id.monitor mon
            JOIN mon.address addr
            WHERE addr.client.id IN :partnerIds
            GROUP BY addr.client.id
            """)
    java.util.List<CountByPartnerRow> countAdsInPartnerMonitors(@Param("partnerIds") java.util.List<java.util.UUID> partnerIds);
}
