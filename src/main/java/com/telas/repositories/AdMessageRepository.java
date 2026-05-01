package com.telas.repositories;

import com.telas.entities.AdMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdMessageRepository extends JpaRepository<AdMessage, UUID> {
    List<AdMessage> findAllByAdIdOrderByCreatedAtAsc(UUID adId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT m FROM AdMessage m
        JOIN FETCH m.ad a
        JOIN FETCH a.client c
        WHERE c.id = :clientId
        ORDER BY m.createdAt ASC
        """)
    List<AdMessage> findAllByClientIdOrderByCreatedAtAsc(@org.springframework.data.repository.query.Param("clientId") UUID clientId);
}

