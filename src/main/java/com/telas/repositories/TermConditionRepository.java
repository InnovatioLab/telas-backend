package com.telas.repositories;

import com.telas.entities.TermCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TermConditionRepository extends JpaRepository<TermCondition, UUID> {
    TermCondition findTopByOrderByUpdatedAtDesc();
}
