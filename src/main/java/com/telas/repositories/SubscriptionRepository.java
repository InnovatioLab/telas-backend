package com.telas.repositories;

import com.telas.entities.Subscription;
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
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID>, JpaSpecificationExecutor<Subscription> {
    @NotNull
    @Override
    @Query("SELECT s FROM Subscription s LEFT JOIN FETCH s.payments JOIN FETCH s.monitors WHERE s.id = :id")
    Optional<Subscription> findById(@NotNull UUID id);

    @Query("""
                SELECT s FROM Subscription s
                JOIN FETCH s.monitors
                WHERE s.client.id = :clientId
                  AND s.status = 'ACTIVE'
                  AND (s.endsAt IS NULL OR s.endsAt > CURRENT_TIMESTAMP)
            """)
    List<Subscription> findActiveSubscriptionsByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT s FROM Subscription s JOIN FETCH s.monitors WHERE s.endsAt IS NOT NULL AND s.endsAt < :now AND s.status = 'ACTIVE' AND s.bonus = false")
    List<Subscription> getActiveAndExpiredSubscriptions(Instant now);

    @Query("""
                SELECT s FROM Subscription s
                WHERE s.endsAt IS NOT NULL
                  AND FUNCTION('date', s.endsAt) = :targetDate
                  AND s.status = 'ACTIVE'
                  AND s.bonus = false
                  AND s.recurrence <> 'MONTHLY'
            """)
    List<Subscription> findSubscriptionsExpiringIn15Days(@Param("targetDate") java.sql.Date targetDate);

    @Query("""
                SELECT s FROM Subscription s
                WHERE s.endsAt IS NOT NULL
                  AND s.endsAt >= :now
                  AND s.endsAt < :tomorrow
                  AND s.status = 'ACTIVE'
                  AND s.bonus = false
                  AND s.recurrence <> 'MONTHLY'
            """)
    List<Subscription> findSubscriptionsExpiringInNext24Hours(
            @Param("now") Instant now,
            @Param("tomorrow") Instant tomorrow
    );

}
