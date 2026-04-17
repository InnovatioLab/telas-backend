package com.telas.monitoring.repositories;

import com.telas.monitoring.entities.BoxScriptUpdateCommandEntity;
import com.telas.monitoring.entities.BoxScriptUpdateCommandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoxScriptUpdateCommandEntityRepository
        extends JpaRepository<BoxScriptUpdateCommandEntity, UUID> {

    boolean existsByBox_IdAndStatus(UUID boxId, BoxScriptUpdateCommandStatus status);

    Optional<BoxScriptUpdateCommandEntity> findFirstByBox_IdAndStatusOrderByCreatedAtAsc(
            UUID boxId, BoxScriptUpdateCommandStatus status);
}
