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
            LEFT JOIN FETCH q.revisions r
            LEFT JOIN FETCH r.answers
            WHERE q.adRequest.id = :adRequestId
            """)
    Optional<BusinessQuestionnaire> findByAdRequestIdWithRevisionsAndAnswers(@Param("adRequestId") UUID adRequestId);

    @Query("""
            SELECT q FROM BusinessQuestionnaire q
            LEFT JOIN FETCH q.revisions r
            LEFT JOIN FETCH r.answers
            WHERE q.id = :id
            """)
    Optional<BusinessQuestionnaire> findByIdWithRevisionsAndAnswers(@Param("id") UUID id);
}
