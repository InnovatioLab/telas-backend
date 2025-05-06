package com.marketingproject.repositories;

import com.marketingproject.entities.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID>, JpaSpecificationExecutor<Attachment> {
    @Query("SELECT a FROM Attachment a WHERE a.id IN :ids")
    Optional<List<Attachment>> findByIdIn(List<UUID> ids);
}
