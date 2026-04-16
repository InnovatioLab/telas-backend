package com.telas.repositories;

import com.telas.entities.Notification;
import com.telas.enums.NotificationReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  @Query("SELECT n FROM Notification n JOIN n.client c WHERE c.id = :clientId ORDER BY n.createdAt DESC")
  List<Notification> findAllByClientIdOrderByCreatedAtDesc(UUID clientId);

  @Query("""
          SELECT n FROM Notification n
          WHERE n.client.id = :clientId AND n.reference IN :refs
          ORDER BY n.createdAt DESC
          """)
  List<Notification> findByClientIdAndReferenceInOrderByCreatedAtDesc(
          @Param("clientId") UUID clientId,
          @Param("refs") Collection<NotificationReference> refs);

  List<Notification> findByIdIn(List<UUID> ids);
}
