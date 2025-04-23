package com.marketingproject.repositories;

import com.marketingproject.entities.Monitor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, UUID> {
    @Override
    @NotNull
    @Query("SELECT m FROM Monitor m LEFT JOIN m.advertisingAttachments WHERE m.id = :id")
    Optional<Monitor> findById(@NotNull UUID id);
}
