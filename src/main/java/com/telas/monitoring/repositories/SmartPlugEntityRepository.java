package com.telas.monitoring.repositories;

import com.telas.monitoring.entities.SmartPlugEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SmartPlugEntityRepository extends JpaRepository<SmartPlugEntity, UUID> {

    @Query(
            "SELECT DISTINCT p FROM SmartPlugEntity p "
                    + "LEFT JOIN FETCH p.monitor m "
                    + "LEFT JOIN FETCH m.box mb "
                    + "LEFT JOIN FETCH p.box bx "
                    + "WHERE p.enabled = true AND (m IS NOT NULL OR bx IS NOT NULL)")
    List<SmartPlugEntity> findAllEnabledForChecks();

    List<SmartPlugEntity> findByEnabledTrue();

    Optional<SmartPlugEntity> findByMonitor_Id(UUID monitorId);

    Optional<SmartPlugEntity> findByBox_Id(UUID boxId);

    Optional<SmartPlugEntity> findByMacAddress(String macAddress);

    List<SmartPlugEntity> findByMonitorIsNullAndBoxIsNullOrderByCreatedAtDesc();

    @Query(
            "SELECT DISTINCT p FROM SmartPlugEntity p "
                    + "LEFT JOIN FETCH p.monitor m "
                    + "LEFT JOIN FETCH p.box b "
                    + "ORDER BY p.createdAt DESC")
    List<SmartPlugEntity> findAllWithMonitor();
}
