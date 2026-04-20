package com.telas.monitoring.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SmartPlugCheckRunRepository extends Repository<com.telas.monitoring.entities.CheckRunEntity, UUID> {

    interface SmartPlugLastReadingRow {
        UUID getSmartPlugId();

        Instant getStartedAt();

        Boolean getSuccess();

        String getErrorMessage();

        Boolean getRelayOn();

        Double getPowerWatts();

        Double getVoltageVolts();

        Double getCurrentAmperes();
    }

    interface SmartPlugHistoryRow {
        Instant getStartedAt();

        Boolean getSuccess();

        String getErrorMessage();

        Boolean getRelayOn();

        Double getPowerWatts();

        Double getVoltageVolts();

        Double getCurrentAmperes();
    }

    @Query(
            value =
                    """
                    SELECT
                        cr.smart_plug_id AS smartPlugId,
                        cr.started_at AS startedAt,
                        cr.success AS success,
                        cr.error_message AS errorMessage,
                        (cr.metadata_json->>'relayOn')::boolean AS relayOn,
                        (cr.metadata_json->>'powerWatts')::double precision AS powerWatts,
                        (cr.metadata_json->>'voltageVolts')::double precision AS voltageVolts,
                        (cr.metadata_json->>'currentAmperes')::double precision AS currentAmperes
                    FROM monitoring.check_runs cr
                    JOIN (
                        SELECT smart_plug_id, MAX(started_at) AS max_started_at
                        FROM monitoring.check_runs
                        WHERE smart_plug_id IS NOT NULL
                        GROUP BY smart_plug_id
                    ) last_run
                        ON last_run.smart_plug_id = cr.smart_plug_id
                        AND last_run.max_started_at = cr.started_at
                    """,
            nativeQuery = true)
    List<SmartPlugLastReadingRow> findLastReadingsForAllPlugs();

    @Query(
            value =
                    """
                    SELECT
                        cr.started_at AS startedAt,
                        cr.success AS success,
                        cr.error_message AS errorMessage,
                        (cr.metadata_json->>'relayOn')::boolean AS relayOn,
                        (cr.metadata_json->>'powerWatts')::double precision AS powerWatts,
                        (cr.metadata_json->>'voltageVolts')::double precision AS voltageVolts,
                        (cr.metadata_json->>'currentAmperes')::double precision AS currentAmperes
                    FROM monitoring.check_runs cr
                    WHERE cr.smart_plug_id = :plugId
                      AND (:fromTs IS NULL OR cr.started_at >= :fromTs)
                      AND (:toTs IS NULL OR cr.started_at <= :toTs)
                    ORDER BY cr.started_at DESC
                    LIMIT :limit
                    """,
            nativeQuery = true)
    List<SmartPlugHistoryRow> findHistory(UUID plugId, Instant fromTs, Instant toTs, int limit);
}

