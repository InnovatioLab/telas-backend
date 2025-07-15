package com.telas.repositories;

import com.telas.entities.BoxAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoxAddressRepository extends JpaRepository<BoxAddress, UUID> {
  @Query("SELECT ba FROM BoxAddress ba WHERE ba.box IS NULL")
  List<BoxAddress> findAllAvailable();

}
