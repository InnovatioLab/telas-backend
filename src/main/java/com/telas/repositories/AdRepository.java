package com.telas.repositories;

import com.telas.dtos.response.AdminAdOperationRowDto;
import com.telas.entities.Ad;
import com.telas.enums.AdValidationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdRepository extends JpaRepository<Ad, UUID>, JpaSpecificationExecutor<Ad> {

	List<Ad> findByClientIdAndValidation(UUID clientId, AdValidationType validation);

	@Query("""
			SELECT a FROM Ad a
			JOIN FETCH a.client
			LEFT JOIN FETCH a.adRequest
			WHERE a.id = :adId
			""")
	java.util.Optional<Ad> findByIdWithClientAndAdRequest(@Param("adId") UUID adId);

	interface ApprovedCountByClientRow {
		UUID getClientId();
		long getApprovedCount();
	}

	@Query("""
		SELECT ad.client.id as clientId, COUNT(ad) as approvedCount
		FROM Ad ad
		WHERE ad.client.id IN :clientIds
		  AND ad.validation = 'APPROVED'
		GROUP BY ad.client.id
		""")
	List<ApprovedCountByClientRow> countApprovedAdsByClientIds(@Param("clientIds") List<UUID> clientIds);

	@Query(
			value = """
					SELECT a.id FROM ads a
					INNER JOIN clients c ON c.id = a.client_id
					WHERE a.validation = 'APPROVED'
					AND a.unused_since IS NOT NULL
					AND NOT EXISTS (SELECT 1 FROM monitors_ads ma WHERE ma.ad_id = a.id)
					AND NOT EXISTS (
						SELECT 1 FROM ad_requests r
						WHERE a.ad_request_id IS NOT NULL AND r.id = a.ad_request_id AND r.active = true
					)
					AND a.unused_since < (
						NOW() - (COALESCE(c.ads_retention_days_override, :globalDays) * INTERVAL '1 DAY')
					)
					""",
			nativeQuery = true)
	List<UUID> findIdsEligibleForRetentionCleanup(@Param("globalDays") int globalDays);

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
	List<Ad> findAllValidAdsForMonitor(@Param("validation") AdValidationType validation, @Param("monitorId") UUID monitorId);

	@Query("""
		SELECT ad FROM Ad ad
		JOIN FETCH ad.client c
		WHERE ad.id IN :ids
		  AND ad.validation = 'APPROVED'
		  AND ad.type <> 'application/pdf'
		""")
	List<Ad> findApprovedNonPdfByIds(@Param("ids") Collection<UUID> ids);

	@Query("""
		SELECT ad FROM Ad ad
		WHERE ad.validation = 'APPROVED'
		  AND ad.id NOT IN (
		      SELECT ma.id.ad.id FROM MonitorAd ma WHERE ma.id.monitor.id = :monitorId
		  )
		AND ad.type <> 'application/pdf'
		  AND (
		      COALESCE(TRIM(:name), '') = ''
		      OR LOWER(ad.name) LIKE CONCAT('%', LOWER(TRIM(:name)), '%')
		      OR LOWER(ad.client.businessName) LIKE CONCAT('%', LOWER(TRIM(:name)), '%')
		  )
		""")
	List<Ad> findAllApprovedNotInMonitorFiltered(@Param("monitorId") UUID monitorId, @Param("name") String name);

	@Query("""
		SELECT COUNT(ad) FROM Ad ad
		WHERE ad.validation = 'APPROVED'
		  AND ad.id NOT IN (
		      SELECT ma.id.ad.id FROM MonitorAd ma WHERE ma.id.monitor.id = :monitorId
		  )
		  AND ad.type <> 'application/pdf'
		""")
	long countAllApprovedNotInMonitor(@Param("monitorId") UUID monitorId);

	@Query(value = "SELECT EXISTS (SELECT 1 FROM ads_attachments WHERE attachment_id = :attachmentId)", nativeQuery = true)
	boolean existsAdReferencingAttachment(@Param("attachmentId") UUID attachmentId);

	@Query("""
			SELECT DISTINCT ar.targetMonitor.id
			FROM Ad ad
			JOIN ad.adRequest ar
			WHERE ad.client.id = :partnerId
			AND ad.validation = com.telas.enums.AdValidationType.APPROVED
			AND ar.targetMonitor IS NOT NULL
			AND NOT EXISTS (
			    SELECT 1 FROM MonitorAd ma
			    WHERE ma.id.ad.id = ad.id AND ma.id.monitor.id = ar.targetMonitor.id
			)
			""")
	List<UUID> findDistinctPendingTargetMonitorIdsForPartner(@Param("partnerId") UUID partnerId);

	@Query("""
			SELECT ad FROM Ad ad
			JOIN FETCH ad.adRequest ar
			WHERE ad.client.id = :partnerId
			AND ad.validation = com.telas.enums.AdValidationType.APPROVED
			AND ar.targetMonitor.id = :monitorId
			AND NOT EXISTS (
			    SELECT 1 FROM MonitorAd ma
			    WHERE ma.id.ad.id = ad.id AND ma.id.monitor.id = :monitorId
			)
			""")
	List<Ad> findApprovedPartnerAdsPendingPlaylistOnMonitor(
			@Param("partnerId") UUID partnerId,
			@Param("monitorId") UUID monitorId);

}