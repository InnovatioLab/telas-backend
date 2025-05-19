package com.telas.repositories;

import com.telas.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  @Query("SELECT p FROM Payment p WHERE p.stripePaymentId = :stripePaymentId")
  Optional<Payment> findByStripePaymentId(String stripePaymentId);
}
