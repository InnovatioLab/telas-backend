package com.telas.repositories;

import com.telas.entities.SubscriptionFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubscriptionFlowRepository extends JpaRepository<SubscriptionFlow, UUID> {
}
