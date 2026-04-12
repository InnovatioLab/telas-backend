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
            "SELECT DISTINCT p FROM SmartPlugEntity p JOIN FETCH p.monitor m JOIN FETCH m.box b WHERE p.enabled = true")
    List<SmartPlugEntity> findAllEnabledWithMonitorAndBox();

    List<SmartPlugEntity> findByEnabledTrue();

    Optional<SmartPlugEntity> findByMonitor_Id(UUID monitorId);

    Optional<SmartPlugEntity> findByMacAddress(String macAddress);

    @Query("SELECT p FROM SmartPlugEntity p JOIN FETCH p.monitor ORDER BY p.createdAt DESC")
    List<SmartPlugEntity> findAllWithMonitor();
}
