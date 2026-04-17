package com.telas.repositories;

import com.telas.entities.AdminEmailAlertPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminEmailAlertPreferenceRepository extends JpaRepository<AdminEmailAlertPreference, UUID> {

    List<AdminEmailAlertPreference> findAllByClient_Id(UUID clientId);

    Optional<AdminEmailAlertPreference> findByClient_IdAndAlertCategory(UUID clientId, String alertCategory);

    @Modifying
    @Query("DELETE FROM AdminEmailAlertPreference p WHERE p.client.id = :clientId")
    void deleteByClient_Id(@Param("clientId") UUID clientId);
}
