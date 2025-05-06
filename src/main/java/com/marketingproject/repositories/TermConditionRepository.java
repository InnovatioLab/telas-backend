package com.marketingproject.repositories;

import com.marketingproject.entities.TermCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TermConditionRepository extends JpaRepository<TermCondition, UUID> {
    @Query("SELECT t FROM TermCondition t ORDER BY t.updatedAt DESC")
    TermCondition findLatest();
}
