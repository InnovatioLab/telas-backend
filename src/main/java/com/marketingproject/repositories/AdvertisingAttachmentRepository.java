package com.marketingproject.repositories;

import com.marketingproject.entities.AdvertisingAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdvertisingAttachmentRepository extends JpaRepository<AdvertisingAttachment, UUID> {
    @Query("SELECT a FROM AdvertisingAttachment a WHERE a.id IN :id")
    Optional<List<AdvertisingAttachment>> findAllByIdIn(List<UUID> ids);
}
