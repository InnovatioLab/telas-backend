package com.telas.monitoring.repositories;

import com.telas.monitoring.entities.ApplicationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ApplicationLogEntityRepository extends JpaRepository<ApplicationLogEntity, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ApplicationLogEntity e WHERE e.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
