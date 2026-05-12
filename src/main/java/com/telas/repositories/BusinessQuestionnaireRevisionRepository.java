package com.telas.repositories;

import com.telas.entities.BusinessQuestionnaireRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BusinessQuestionnaireRevisionRepository extends JpaRepository<BusinessQuestionnaireRevision, UUID> {

    Optional<BusinessQuestionnaireRevision> findTopByQuestionnaire_IdOrderByVersionDesc(UUID questionnaireId);
}
