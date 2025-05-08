package com.telas.repositories;

import com.telas.entities.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    @Query("SELECT a FROM Address a WHERE a.id = :id AND a.client.role = 'PARTNER'")
    Optional<Address> findAddressPartnerById(UUID id);

    @Query("SELECT a FROM Address a WHERE a.zipCode = :zipCode")
    List<Address> findByZipCode(String zipCode);
}
