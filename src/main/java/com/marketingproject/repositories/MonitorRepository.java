package com.marketingproject.repositories;

import com.marketingproject.entities.Monitor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, UUID> {
    @Override
    @NotNull
    @Query("SELECT m FROM Monitor m LEFT JOIN FETCH m.monitorAdvertisingAttachments WHERE m.id = :id")
    Optional<Monitor> findById(@NotNull UUID id);

    @Query("SELECT m FROM Monitor m LEFT JOIN m.monitorAdvertisingAttachments maa WHERE maa.id.advertisingAttachment.id = :advertisingAttachmentId")
    List<Monitor> findByAdvertisingAttachmentId(UUID advertisingAttachmentId);

    @Query(value = """
                SELECT m.id, m.fl_active, m.type, m.size_in_inches, m.latitude, m.longitude,
                       ROUND(
                           CAST(
                               ST_Distance(
                                   ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                                   ST_SetSRID(ST_MakePoint(m.longitude, m.latitude), 4326)::geography
                               ) / 1000 AS numeric
                           ), 2
                       ) AS distance
                FROM monitors m
                WHERE m.fl_active = TRUE
                  AND (:size IS NULL OR m.size_in_inches >= :size)
                  AND (:type IS NULL OR m.type = :type)
                ORDER BY distance
                LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findNearestActiveMonitorsWithFilters(double latitude, double longitude, BigDecimal size, String type, int limit);
}
