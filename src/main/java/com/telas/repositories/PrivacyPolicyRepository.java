package com.telas.repositories;

import com.telas.entities.PrivacyPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PrivacyPolicyRepository extends JpaRepository<PrivacyPolicy, UUID> {
  @Query("SELECT p FROM PrivacyPolicy p ORDER BY p.updatedAt DESC")
  PrivacyPolicy findLatest();
}
