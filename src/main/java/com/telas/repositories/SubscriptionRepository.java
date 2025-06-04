package com.telas.repositories;

import com.telas.entities.Subscription;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID>, JpaSpecificationExecutor<Subscription> {
  @NotNull
  @Override
  @Query("SELECT s FROM Subscription s JOIN FETCH s.payments WHERE s.id = :id")
  Optional<Subscription> findById(@NotNull UUID id);

  @Query("SELECT s FROM Subscription s WHERE s.endsAt IS NOT NULL AND s.endsAt < :now AND s.status = 'ACTIVE' AND s.bonus = false")
  List<Subscription> getActiveAndExpiredSubscriptions(Instant now);
}
