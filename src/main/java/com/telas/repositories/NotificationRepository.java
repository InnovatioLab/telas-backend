package com.telas.repositories;

import com.telas.entities.Client;
import com.telas.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @Query("SELECT n FROM Notification n WHERE n.client = :client ORDER BY n.createdAt DESC")
    List<Notification> findAllByClientOrderByCreatedAtDesc(Client client);

    List<Notification> findByIdIn(List<UUID> ids);
}
