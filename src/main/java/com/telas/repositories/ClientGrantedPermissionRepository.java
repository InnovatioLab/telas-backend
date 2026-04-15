package com.telas.repositories;

import com.telas.entities.ClientGrantedPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientGrantedPermissionRepository extends JpaRepository<ClientGrantedPermission, UUID> {

    List<ClientGrantedPermission> findByClient_Id(UUID clientId);

    @Modifying
    @Query("DELETE FROM ClientGrantedPermission g WHERE g.client.id = :clientId")
    void deleteByClient_Id(@Param("clientId") UUID clientId);

    boolean existsByClient_IdAndPermissionCode(UUID clientId, String permissionCode);
}
