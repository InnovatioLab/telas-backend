package com.telas.repositories;

import com.telas.entities.Ad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdRepository extends JpaRepository<Ad, UUID>, JpaSpecificationExecutor<Ad> {
  @Query("SELECT a FROM Ad a WHERE a.id IN :ids")
  Optional<List<Ad>> findAllById(List<UUID> ids);

  @Query("SELECT a FROM Ad a JOIN a.monitorAds ma WHERE ma.id.monitor.id IN :monitorIds AND a.client.id = :clientId")
  List<Ad> findAdsByMonitorIdsAndClientId(@Param("monitorIds") List<UUID> monitorIds, @Param("clientId") UUID clientId);
}