package com.telas.repositories;

import com.telas.entities.BusinessQuestionnaireRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BusinessQuestionnaireRevisionRepository extends JpaRepository<BusinessQuestionnaireRevision, UUID> {

    Optional<BusinessQuestionnaireRevision> findTopByQuestionnaire_IdOrderByVersionDesc(UUID questionnaireId);

    @Query("""
            SELECT r FROM BusinessQuestionnaireRevision r
            LEFT JOIN FETCH r.answers
            WHERE r.questionnaire.id = :questionnaireId
            AND r.version = (
                SELECT MAX(r2.version) FROM BusinessQuestionnaireRevision r2
                WHERE r2.questionnaire.id = :questionnaireId
            )
            """)
    Optional<BusinessQuestionnaireRevision> findLatestWithAnswersByQuestionnaireId(
            @Param("questionnaireId") UUID questionnaireId);
}
