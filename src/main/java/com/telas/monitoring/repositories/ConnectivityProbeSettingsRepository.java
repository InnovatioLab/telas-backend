package com.telas.monitoring.repositories;

import com.telas.monitoring.entities.ConnectivityProbeSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConnectivityProbeSettingsRepository extends JpaRepository<ConnectivityProbeSettingsEntity, Short> {}
