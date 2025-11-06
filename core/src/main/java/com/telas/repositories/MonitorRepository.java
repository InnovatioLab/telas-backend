package com.telas.repositories;

import com.telas.entities.Monitor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, UUID>, JpaSpecificationExecutor<Monitor> {
    @Override
    @NotNull
    @Query("SELECT m FROM Monitor m LEFT JOIN FETCH m.monitorAds WHERE m.id = :id")
    Optional<Monitor> findById(@NotNull UUID id);

    @Query("""
                SELECT m FROM Monitor m
                JOIN m.address a
                WHERE a.zipCode = :zipCode
                  AND m.active = true
                  AND m.box IS NOT NULL
                  AND NOT EXISTS (
                    SELECT 1 FROM Subscription s
                    JOIN s.monitors sm
                    WHERE sm.id = m.id
                      AND s.client.id = :clientId
                      AND s.status = 'ACTIVE'
                      AND (s.endsAt IS NULL OR s.endsAt > CURRENT_TIMESTAMP)
                  )
            """)
    List<Monitor> findAvailableMonitorsByZipCode(@Param("zipCode") String zipCode, @Param("clientId") UUID clientId);


    @Query("SELECT m FROM Monitor m LEFT JOIN FETCH m.monitorAds ma WHERE m.id IN :monitorIds")
    List<Monitor> findAllByIdIn(List<UUID> monitorIds);

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
}
