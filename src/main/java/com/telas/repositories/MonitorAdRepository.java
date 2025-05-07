package com.telas.repositories;

import com.telas.entities.MonitorAd;
import com.telas.entities.MonitorAdPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MonitorAdvertisingAttachmentRepository extends JpaRepository<MonitorAd, MonitorAdPK>, JpaSpecificationExecutor<MonitorAd> {
    @Query("SELECT maa FROM MonitorAd maa WHERE maa.id.advertisingAttachment.id = :advertisingAttachmentId")
    List<MonitorAd> findByAdvertisingAttachmentId(UUID advertisingAttachmentId);
}