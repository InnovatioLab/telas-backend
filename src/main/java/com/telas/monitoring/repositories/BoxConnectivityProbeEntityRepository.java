package com.telas.monitoring.repositories;

import com.telas.monitoring.entities.BoxConnectivityProbeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BoxConnectivityProbeEntityRepository extends JpaRepository<BoxConnectivityProbeEntity, UUID> {}
