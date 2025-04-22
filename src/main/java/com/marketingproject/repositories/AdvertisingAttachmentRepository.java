package com.marketingproject.repositories;

import com.marketingproject.entities.AdvertisingAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdvertisingAttachmentRepository extends JpaRepository<AdvertisingAttachment, UUID> {
}
