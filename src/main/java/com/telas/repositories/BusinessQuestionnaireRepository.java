package com.telas.repositories;

import com.telas.entities.BusinessQuestionnaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BusinessQuestionnaireRepository extends JpaRepository<BusinessQuestionnaire, UUID> {

    Optional<BusinessQuestionnaire> findByAdRequest_Id(UUID adRequestId);

    @Query("""
            SELECT q FROM BusinessQuestionnaire q
            WHERE q.client.id = :clientId AND q.adRequest IS NULL
            """)
    Optional<BusinessQuestionnaire> findDraftByClientId(@Param("clientId") UUID clientId);

    @Query("""
            SELECT q FROM BusinessQuestionnaire q
            JOIN FETCH q.client
            WHERE q.adRequest.id = :adRequestId
            """)
    Optional<BusinessQuestionnaire> findByAdRequestIdWithClient(@Param("adRequestId") UUID adRequestId);

    @Query("""
            SELECT q FROM BusinessQuestionnaire q
            JOIN FETCH q.client
            JOIN FETCH q.adRequest ar
            LEFT JOIN FETCH ar.ad
            WHERE q.adRequest.id = :adRequestId
            """)
    Optional<BusinessQuestionnaire> findByAdRequestIdWithClientAndAd(@Param("adRequestId") UUID adRequestId);
}
