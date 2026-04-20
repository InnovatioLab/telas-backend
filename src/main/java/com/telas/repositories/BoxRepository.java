package com.telas.repositories;

import com.telas.entities.Box;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoxRepository extends JpaRepository<Box, UUID> {
  @NotNull
  @Override
  @Query("SELECT b FROM Box b LEFT JOIN FETCH b.monitors")
  List<Box> findAll();

  @Query("SELECT b FROM Box b LEFT JOIN FETCH b.monitors JOIN b.boxAddress ba WHERE ba.ip = :address OR ba.mac = :address")
  Optional<Box> findByAddress(String address);

  @Override
  @NotNull
  @Query("SELECT b FROM Box b LEFT JOIN FETCH b.monitors JOIN b.boxAddress ba WHERE b.id = :boxId")
  Optional<Box> findById(@NotNull @Param("boxId") UUID boxId);

  @Query(
      "SELECT DISTINCT b FROM Box b JOIN FETCH b.boxAddress LEFT JOIN FETCH b.monitors m LEFT JOIN FETCH m.address")
  List<Box> findAllForTestingOverview();

  @Query(
      "SELECT DISTINCT b FROM Box b JOIN FETCH b.boxAddress LEFT JOIN FETCH b.monitors "
              + "WHERE b.active = true AND b.createdAt < :graceCutoff "
              + "AND NOT EXISTS (SELECT 1 FROM BoxHeartbeatEntity h WHERE h.box.id = b.id)")
  List<Box> findActiveBoxesWithoutHeartbeatAfterGrace(@Param("graceCutoff") Instant graceCutoff);

  @Query("SELECT DISTINCT b FROM Box b JOIN FETCH b.boxAddress WHERE b.active = true")
  List<Box> findAllActiveWithBoxAddress();
}
