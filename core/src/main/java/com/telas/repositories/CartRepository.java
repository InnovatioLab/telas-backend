package com.telas.repositories;

import com.telas.entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
  @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.id = :id")
  Optional<Cart> findByIdWithItens(@Param("id") UUID id);

  @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.client.id = :clientId")
  Optional<Cart> findByClientIdWithItens(@Param("clientId") UUID clientId);

  @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.client.id = :clientId AND c.active = true")
  Optional<Cart> findActiveByClientIdWithItens(@Param("clientId") UUID clientId);

//  @Query("SELECT DISTINCT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.active IS TRUE AND c.client.id = :id")
//  Optional<Cart> findActiveByClientIdWithItens(@Param("id") UUID id);
}
