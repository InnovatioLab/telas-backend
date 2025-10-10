package com.telas.repositories;

import com.telas.entities.Client;
import com.telas.entities.Monitor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {
    @NotNull
    @Override
    @Query("SELECT c FROM Client c JOIN FETCH c.addresses WHERE c.id = :id")
    Optional<Client> findById(@NotNull UUID id);

    @Query("SELECT c FROM Client c JOIN FETCH c.addresses WHERE c.id = :id AND c.status = 'ACTIVE'")
    Optional<Client> findActiveById(UUID id);

    @Query("SELECT c FROM Client c JOIN FETCH c.addresses LEFT JOIN c.attachments LEFT JOIN c.ads LEFT JOIN c.subscriptions WHERE c.id = :id AND c.status = 'ACTIVE'")
    Optional<Client> findActiveIdFromToken(UUID id);

    @Query("SELECT c FROM Client c JOIN FETCH c.addresses LEFT JOIN c.attachments LEFT JOIN c.ads LEFT JOIN c.subscriptions WHERE c.contact.email = :email")
    Optional<Client> findByEmail(String email);

    @Query("SELECT c FROM Client c WHERE c.role = 'ADMIN'")
    List<Client> findAllAdmins();

    @Query("SELECT c FROM Client c JOIN FETCH c.wishlist w LEFT JOIN FETCH w.monitors m WHERE m IN :monitors")
    List<Client> findAllByMonitorsInWishlist(Set<Monitor> monitors);
}
