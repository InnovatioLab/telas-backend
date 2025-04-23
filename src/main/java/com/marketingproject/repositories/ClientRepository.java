package com.marketingproject.repositories;

import com.marketingproject.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM Client c " +
           "WHERE c.identificationNumber = :identificationNumber")
    boolean existsByIdentificationNumber(String identificationNumber);

    @Query("SELECT c FROM Client c JOIN FETCH c.addresses LEFT JOIN FETCH c.attachments LEFT JOIN FETCH c.advertisingAttachments LEFT JOIN FETCH c.notifications WHERE c.id = :id AND c.status = 'ACTIVE'")
    Optional<Client> findActiveById(UUID id);


    @Query("SELECT c FROM Client c JOIN FETCH c.addresses LEFT JOIN FETCH c.attachments LEFT JOIN FETCH c.advertisingAttachments LEFT JOIN FETCH c.notifications WHERE c.identificationNumber = :identificationNumber AND c.status = 'ACTIVE'")
    Optional<Client> findActiveByIdentificationNumber(String identificationNumber);

    @Query("SELECT c FROM Client c JOIN FETCH c.addresses LEFT JOIN FETCH c.attachments LEFT JOIN FETCH c.advertisingAttachments LEFT JOIN FETCH c.notifications WHERE c.identificationNumber = :identificationNumber")
    Optional<Client> findByIdentificationNumber(String identificationNumber);
}
