package com.telas.repositories;

import com.telas.entities.Address;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
  @Override
  @NotNull
  @Query("SELECT a FROM Address a LEFT JOIN a.monitors WHERE a.id = :id")
  Optional<Address> findById(@NotNull UUID id);

  @Query("SELECT a FROM Address a WHERE a.zipCode = :zipCode")
  List<Address> findByZipCode(String zipCode);

  @Query("SELECT a FROM Address a WHERE a.street = :street AND a.city = :city AND a.state = :state AND a.zipCode = :zipCode AND (a.client IS NULL OR a.client.role = 'PARTNER')")
  Optional<Address> findByStreetAndCityAndStateAndZipCode(String street, String city, String state, String zipCode);

  @Query("SELECT a FROM Address a WHERE a.street = :street AND a.city = :city AND a.state = :state AND a.zipCode = :zipCode AND a.client.id = :clientId")
  Optional<Address> findByStreetAndCityAndStateAndZipCodeAndClientId(String street, String city, String state, String zipCode, UUID clientId);
}
