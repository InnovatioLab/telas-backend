package com.telas.repositories;

import com.telas.entities.Subscription;
import com.telas.entities.SubscriptionMonitor;
import com.telas.entities.SubscriptionMonitorPK;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionMonitorRepository extends JpaRepository<SubscriptionMonitor, SubscriptionMonitorPK> {
    @Query("""
            SELECT sm FROM SubscriptionMonitor sm
            JOIN FETCH sm.id.subscription s
            WHERE sm.id.monitor.id = :monitorId
            AND s.status = 'ACTIVE'
            AND (s.endsAt IS NULL OR s.endsAt > CURRENT_TIMESTAMP)
            """)
    List<SubscriptionMonitor> findByMonitorId(@NotNull UUID monitorId);


    @Query("""
            SELECT sm FROM SubscriptionMonitor sm
            JOIN FETCH sm.id.subscription s
            WHERE s.client.id = :clientId
            AND s.status = 'ACTIVE'
            AND (s.endsAt IS NULL OR s.endsAt > CURRENT_TIMESTAMP)
            """)
    List<SubscriptionMonitor> findByClientId(@NotNull UUID clientId);
}
