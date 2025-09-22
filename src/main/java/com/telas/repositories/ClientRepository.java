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
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Client c " +
            "WHERE c.identificationNumber = :identificationNumber")
    boolean existsByIdentificationNumber(String identificationNumber);

    @NotNull
    @Override
    @Query("SELECT c FROM Client c JOIN FETCH c.addresses WHERE c.id = :id")
    Optional<Client> findById(@NotNull UUID id);

    @Query("SELECT c FROM Client c JOIN FETCH c.addresses WHERE c.id = :id AND c.status = 'ACTIVE'")
    Optional<Client> findActiveById(UUID id);

    @Query("SELECT c FROM Client c JOIN FETCH c.addresses LEFT JOIN c.attachments LEFT JOIN c.ads LEFT JOIN c.subscriptions WHERE c.id = :id AND c.status = 'ACTIVE'")
    Optional<Client> findActiveIdFromToken(UUID id);

    @Query(value = """
            SELECT c.* 
            FROM clients c 
            INNER JOIN addresses ad ON c.id = ad.client_id 
            LEFT JOIN ad_requests ar ON c.id = ar.client_id 
            WHERE c.identification_number = :identificationNumber AND c.status = 'ACTIVE'
            """, nativeQuery = true)
    Optional<Client> findActiveByIdentificationNumber(String identificationNumber);


    @Query(value = """
            SELECT c.* 
            FROM clients c 
            INNER JOIN addresses ad ON c.id = ad.client_id 
            LEFT JOIN ad_requests ar ON c.id = ar.client_id 
            WHERE c.identification_number = :identificationNumber 
            """, nativeQuery = true)
    Optional<Client> findByIdentificationNumber(String identificationNumber);

    @Query("SELECT c FROM Client c WHERE c.role = 'ADMIN'")
    List<Client> findAllAdmins();

    @Query("SELECT c FROM Client c JOIN FETCH c.wishlist w LEFT JOIN FETCH w.monitors m WHERE m IN :monitors")
    List<Client> findAllByMonitorsInWishlist(Set<Monitor> monitors);
}
