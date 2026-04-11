package com.telas.monitoring.repositories;

import com.telas.monitoring.entities.BoxHeartbeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoxHeartbeatEntityRepository extends JpaRepository<BoxHeartbeatEntity, UUID> {

    Optional<BoxHeartbeatEntity> findByBox_Id(UUID boxId);

    @Query("""
            SELECT h FROM BoxHeartbeatEntity h
            JOIN FETCH h.box b
            JOIN FETCH b.boxAddress
            WHERE h.lastSeenAt < :cutoff
            """)
    List<BoxHeartbeatEntity> findStaleHeartbeats(@Param("cutoff") Instant cutoff);
}
