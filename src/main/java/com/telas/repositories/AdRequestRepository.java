package com.telas.repositories;

import com.telas.entities.AdRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.telas.enums.AdRequestOrigin;
import com.telas.enums.PartnerSubmissionMode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdRequestRepository extends JpaRepository<AdRequest, UUID>, JpaSpecificationExecutor<AdRequest> {
    @Query("SELECT ar FROM AdRequest ar WHERE ar.isActive = true")
    List<AdRequest> findAllActive();

    boolean existsByClientIdAndRequestOriginAndTargetMonitorIdAndIsActiveTrueAndSubmissionMode(
            UUID clientId,
            AdRequestOrigin requestOrigin,
            UUID targetMonitorId,
            PartnerSubmissionMode submissionMode);

    boolean existsByClientId(UUID clientId);

    @Query("""
        SELECT ar FROM AdRequest ar
        LEFT JOIN FETCH ar.client c
        LEFT JOIN FETCH ar.ad a
        WHERE (:clientId IS NULL OR c.id = :clientId)
        ORDER BY ar.createdAt DESC
        """)
    Page<AdRequest> findForFlowView(@Param("clientId") UUID clientId, Pageable pageable);
}