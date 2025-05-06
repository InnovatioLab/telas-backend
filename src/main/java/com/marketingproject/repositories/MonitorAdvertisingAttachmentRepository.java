package com.marketingproject.repositories;

import com.marketingproject.entities.MonitorAdvertisingAttachment;
import com.marketingproject.entities.MonitorAdvertisingAttachmentPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MonitorAdvertisingAttachmentRepository extends JpaRepository<MonitorAdvertisingAttachment, MonitorAdvertisingAttachmentPK>, JpaSpecificationExecutor<MonitorAdvertisingAttachment> {
    @Query("SELECT maa FROM MonitorAdvertisingAttachment maa WHERE maa.id.advertisingAttachment.id = :advertisingAttachmentId")
    List<MonitorAdvertisingAttachment> findByAdvertisingAttachmentId(UUID advertisingAttachmentId);
}