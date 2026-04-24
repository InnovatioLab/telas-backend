package com.telas.repositories;

import com.telas.entities.Address;
import com.telas.enums.DefaultStatus;
import com.telas.enums.Role;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    @Override
    @NotNull
    @Query("SELECT a FROM Address a LEFT JOIN a.client WHERE a.id = :id")
    Optional<Address> findById(@NotNull UUID id);

    @Query("SELECT a FROM Address a WHERE a.zipCode = :zipCode")
    List<Address> findByZipCode(String zipCode);

    @Query("""
        SELECT a
        FROM Address a
        JOIN a.client c
        WHERE LOWER(a.street) = :street
          AND LOWER(a.city) = :city
          AND LOWER(a.state) = :state
          AND a.zipCode = :zipCode
          AND COALESCE(LOWER(TRIM(a.address2)), '') = :address2
          AND c.status = :clientStatus
        """)
    Optional<Address> findActiveClientConflictByAddressData(
        String street,
        String city,
        String state,
        String zipCode,
        String address2,
        DefaultStatus clientStatus
    );

    @Query("""
        SELECT a FROM Address a
        WHERE LOWER(a.street) = :street
          AND LOWER(a.city) = :city
          AND LOWER(a.state) = :state
          AND a.zipCode = :zipCode
          AND COALESCE(LOWER(TRIM(a.address2)), '') = :address2
        """)
    Optional<Address> findByStreetAndCityAndStateAndZipCode(
        String street,
        String city,
        String state,
        String zipCode,
        String address2
    );

    @Query("""
        SELECT a
        FROM Address a
        JOIN a.client c
        WHERE c.role = :role
          AND c.status = :clientStatus
          AND NOT EXISTS (
            SELECT 1 FROM Monitor m
            WHERE m.address.id = a.id
          )
        ORDER BY LOWER(c.businessName) ASC, LOWER(a.street) ASC
        """)
    List<Address> findAvailablePartnerAddresses(
        @Param("role") Role role,
        @Param("clientStatus") DefaultStatus clientStatus
    );

    @Query("""
        SELECT a
        FROM Address a
        JOIN a.client c
        WHERE c.role = :role
          AND c.status = :clientStatus
          AND NOT EXISTS (
            SELECT 1 FROM Monitor m
            WHERE m.address.id = a.id
          )
          AND (
            :q IS NULL
            OR TRIM(:q) = ''
            OR LOWER(c.businessName) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(c.contact.email) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(a.street) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(a.city) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(a.state) LIKE LOWER(CONCAT('%', :q, '%'))
            OR a.zipCode LIKE CONCAT('%', :q, '%')
          )
        ORDER BY LOWER(c.businessName) ASC, LOWER(a.street) ASC
        """)
    List<Address> findAvailablePartnerAddressesFiltered(
        @Param("role") Role role,
        @Param("clientStatus") DefaultStatus clientStatus,
        @Param("q") String q
    );
}
