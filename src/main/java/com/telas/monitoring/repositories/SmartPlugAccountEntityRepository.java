package com.telas.monitoring.repositories;

import com.telas.monitoring.entities.SmartPlugAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SmartPlugAccountEntityRepository extends JpaRepository<SmartPlugAccountEntity, UUID> {
    Optional<SmartPlugAccountEntity> findByBox_IdAndVendorAndEnabledTrue(UUID boxId, String vendor);
}

