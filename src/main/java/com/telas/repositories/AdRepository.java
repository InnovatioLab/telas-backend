package com.telas.repositories;

import com.telas.entities.Ad;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdRepository extends JpaRepository<Ad, UUID>, JpaSpecificationExecutor<Ad> {
  @Query("SELECT a FROM Ad a WHERE a.id IN :ids")
  Optional<List<Ad>> findAllById(List<UUID> ids);

  @NotNull
  @Override
  @Query("SELECT a FROM Ad a LEFT JOIN FETCH a.refusedAds WHERE a.id = :id")
  Optional<Ad> findById(@NotNull UUID id);
}