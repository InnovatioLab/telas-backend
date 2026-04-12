package com.telas.monitoring.repositories;

import com.telas.monitoring.entities.IncidentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IncidentEntityRepository extends JpaRepository<IncidentEntity, UUID> {

    Page<IncidentEntity> findAllByOrderByOpenedAtDesc(Pageable pageable);

    boolean existsByMonitor_IdAndIncidentTypeAndClosedAtIsNull(UUID monitorId, String incidentType);
}
