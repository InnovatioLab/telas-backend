package com.telas.repositories;

import com.telas.dtos.response.MonitorBoxMinResponseDto;
import com.telas.dtos.response.MonitorValidationResponseDto;
import com.telas.entities.Monitor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, UUID>, JpaSpecificationExecutor<Monitor> {
  @Override
  @NotNull
  @Query("SELECT m FROM Monitor m LEFT JOIN FETCH m.monitorAds WHERE m.id = :id")
  Optional<Monitor> findById(@NotNull UUID id);

//  @Query(value = """
//              SELECT m.id, 
//                     m.fl_active, 
//                     m.type, 
//                     m.size_in_inches, 
//                     a.latitude, 
//                     a.longitude,
//                     ROUND(
//                         CAST(
//                             ST_Distance(
//                                 ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
//                                 ST_SetSRID(ST_MakePoint(a.longitude, a.latitude), 4326)::geography
//                             ) / 1000 AS numeric
//                         ), 2
//                     ) AS distance,
//                     CASE 
//                         WHEN (SELECT COUNT(*) FROM monitors_ads ma WHERE ma.monitor_id = m.id) < m.max_blocks 
//                         THEN true 
//                         ELSE false 
//                     END AS has_available_slots,
//                     (SELECT MIN(s.ends_at) 
//                      FROM subscriptions s 
//                      WHERE s.id IN (
//                          SELECT sm.subscription_id 
//                          FROM subscriptions_monitors sm 
//                          WHERE sm.monitor_id = m.id
//                      ) 
//                      AND s.recurrence != 'MONTHLY' AND s.status = 'ACTIVE') AS estimated_slot_release_date
//              FROM monitors m
//              JOIN addresses a ON m.address_id = a.id
//              WHERE m.fl_active = TRUE
//               AND m.box_id IS NOT NULL
//                AND (:size IS NULL OR m.size_in_inches >= :size)
//                AND (:type IS NULL OR m.type = :type)
//              ORDER BY distance
//              LIMIT :limit
//          """, nativeQuery = true)
//  List<Object[]> findNearestActiveMonitorsWithFilters(double latitude, double longitude, BigDecimal size, String type, int limit);

  @Query(value = """
              SELECT m.id, 
                     m.fl_active, 
                     m.type, 
                     m.size_in_inches, 
                     a.latitude, 
                     a.longitude,
                     ROUND(
                         CAST(
                             ST_Distance(
                                 ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                                 ST_SetSRID(ST_MakePoint(a.longitude, a.latitude), 4326)::geography
                             ) / 1000 AS numeric
                         ), 2
                     ) AS distance,
                     CASE
                      WHEN
                        (SELECT COUNT(*) FROM monitors_ads ma WHERE ma.monitor_id = m.id) < m.max_blocks
                        AND
                        (SELECT COUNT(*) FROM subscriptions_monitors sm
                          JOIN subscriptions s ON sm.subscription_id = s.id
                          WHERE sm.monitor_id = m.id
                            AND s.status = 'ACTIVE'
                            AND (s.ends_at IS NULL OR s.ends_at > NOW())
                        ) < m.max_blocks
                      THEN true
                      ELSE false
                    END AS has_available_slots,
                     (SELECT MIN(s.ends_at) 
                      FROM subscriptions s 
                      WHERE s.id IN (
                          SELECT sm.subscription_id 
                          FROM subscriptions_monitors sm 
                          WHERE sm.monitor_id = m.id
                      ) 
                      AND s.recurrence != 'MONTHLY' AND s.status = 'ACTIVE') AS estimated_slot_release_date,
                      (SELECT COUNT(*) FROM monitors_ads ma WHERE ma.monitor_id = m.id) AS ads_count
              FROM monitors m
              JOIN addresses a ON m.address_id = a.id
              WHERE m.fl_active = TRUE
                AND m.box_id IS NOT NULL
                AND (:size IS NULL OR m.size_in_inches >= :size)
                AND (:type IS NULL OR m.type = :type)
              ORDER BY distance
              LIMIT :limit
          """, nativeQuery = true)
  List<Object[]> findNearestActiveMonitorsWithFilters(double latitude, double longitude, BigDecimal size, String type, int limit);


  @Query("SELECT m FROM Monitor m LEFT JOIN FETCH m.monitorAds ma WHERE m.id IN :monitorIds")
  List<Monitor> findAllByIdIn(List<UUID> monitorIds);

  @Query("""
              SELECT new com.telas.dtos.response.MonitorValidationResponseDto(
                  m.id,
                  (m.active = true
                   AND m.maxBlocks > (SELECT COUNT(*) FROM MonitorAd ma WHERE ma.id.monitor.id = m.id)
                  ),
                  CASE WHEN EXISTS (
                      SELECT 1 FROM Subscription s
                      JOIN s.monitors sm
                      WHERE sm.id = m.id
                        AND s.client.id = :clientId
                        AND s.status = 'ACTIVE'
                        AND (s.endsAt IS NULL OR s.endsAt > CURRENT_TIMESTAMP)
                  ) THEN true ELSE false END,
                  CASE
                      WHEN (m.box IS NULL OR m.box.active = false) THEN true
                      ELSE false
                  END
              )
              FROM Monitor m
              WHERE m.id IN :monitorIds
          """)
  List<MonitorValidationResponseDto> findInvalidMonitors(@Param("monitorIds") List<UUID> monitorIds, @Param("clientId") UUID clientId);

  @Query("""
              SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
              FROM Subscription s
              JOIN s.monitors m
              WHERE m.id = :monitorId
                AND s.status = 'ACTIVE'
          """)
  boolean existsActiveSubscriptionByMonitorId(@Param("monitorId") UUID monitorId);

  @Query("""
              SELECT m FROM Monitor m
              JOIN m.subscriptions s
              WHERE s.client.id = :clientId
                AND s.status = 'ACTIVE'
                AND (s.endsAt IS NULL OR s.endsAt > CURRENT_TIMESTAMP)
          """)
  List<Monitor> findMonitorsWithActiveSubscriptionsByClientId(@Param("clientId") UUID clientId);

  @Query("""
          SELECT new com.telas.dtos.response.MonitorBoxMinResponseDto(
          m.id,
          CASE
                                WHEN m.box IS NOT NULL  THEN true
                                ELSE false
                            END
          
          ) FROM Monitor m
          """)
  List<MonitorBoxMinResponseDto> findAllMonitorsBox();
}
