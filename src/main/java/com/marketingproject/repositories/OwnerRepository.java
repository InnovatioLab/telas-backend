package com.marketingproject.repositories;

import com.marketingproject.entities.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, UUID> {
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
           "FROM Owner o " +
           "WHERE o.email = :email")
    boolean existsByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
           "FROM Owner o " +
           "WHERE o.identificationNumber = :identificationNumber")
    boolean existsByIdentificationNumber(String identificationNumber);
}
