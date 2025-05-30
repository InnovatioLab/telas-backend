package com.telas.repositories;

import com.telas.entities.Subscription;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
  @NotNull
  @Override
  @Query("SELECT s FROM Subscription s JOIN FETCH s.payments WHERE s.id = :id")
  Optional<Subscription> findById(@NotNull UUID id);
}
