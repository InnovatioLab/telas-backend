package com.telas.repositories;

import com.telas.entities.Box;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoxRepository extends JpaRepository<Box, UUID> {
  @Query("SELECT b FROM Box b LEFT JOIN FETCH b.monitors JOIN b.ip i WHERE i.ipAddress = :ip")
  Optional<Box> findByIp(String ip);

  @Override
  @NotNull
  @Query("SELECT b FROM Box b LEFT JOIN FETCH b.monitors JOIN b.ip i WHERE b.id = :boxId")
  Optional<Box> findById(@NotNull @Param("boxId") UUID boxId);
}
