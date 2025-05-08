package com.telas.repositories;

import com.telas.entities.AdRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdRequestRepository extends JpaRepository<AdRequest, UUID>, JpaSpecificationExecutor<AdRequest> {
    @Query("SELECT ar FROM AdRequest ar WHERE ar.isActive = true")
    List<AdRequest> findAllActive();
}