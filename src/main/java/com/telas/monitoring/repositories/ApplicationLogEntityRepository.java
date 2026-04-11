package com.telas.monitoring.repositories;

import com.telas.monitoring.entities.ApplicationLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(
            value = """
                    SELECT e.* FROM monitoring.application_logs e
                    WHERE (:source IS NULL OR e.source = :source)
                    AND (:level IS NULL OR e.level = :level)
                    AND (CAST(:fromTs AS TIMESTAMPTZ) IS NULL OR e.created_at >= CAST(:fromTs AS TIMESTAMPTZ))
                    AND (CAST(:toTs AS TIMESTAMPTZ) IS NULL OR e.created_at <= CAST(:toTs AS TIMESTAMPTZ))
                    AND (CAST(:qPattern AS TEXT) IS NULL OR e.message ILIKE CAST(:qPattern AS TEXT)
                         OR COALESCE(CAST(e.metadata_json AS TEXT), '') ILIKE CAST(:qPattern AS TEXT))
                    ORDER BY e.created_at DESC
                    """,
            countQuery = """
                    SELECT count(*) FROM monitoring.application_logs e
                    WHERE (:source IS NULL OR e.source = :source)
                    AND (:level IS NULL OR e.level = :level)
                    AND (CAST(:fromTs AS TIMESTAMPTZ) IS NULL OR e.created_at >= CAST(:fromTs AS TIMESTAMPTZ))
                    AND (CAST(:toTs AS TIMESTAMPTZ) IS NULL OR e.created_at <= CAST(:toTs AS TIMESTAMPTZ))
                    AND (CAST(:qPattern AS TEXT) IS NULL OR e.message ILIKE CAST(:qPattern AS TEXT)
                         OR COALESCE(CAST(e.metadata_json AS TEXT), '') ILIKE CAST(:qPattern AS TEXT))
                    """,
            nativeQuery = true)
    Page<ApplicationLogEntity> search(
            @Param("source") String source,
            @Param("level") String level,
            @Param("fromTs") Instant fromTs,
            @Param("toTs") Instant toTs,
            @Param("qPattern") String qPattern,
            Pageable pageable);
}
