package com.telas.shared.audit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.envers.RevisionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CustomRevisionListener implements RevisionListener {
    private static String oldData;
    private static String username;

    @PersistenceContext
    private EntityManager entityManager;

    public static synchronized String getOldData() {
        return oldData;
    }

    public static synchronized void setOldData(String oldData) {
        CustomRevisionListener.oldData = oldData;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        CustomRevisionListener.username = username;
    }

    @Override
    public void newRevision(Object entity) {
        Audit revision = (Audit) entity;
        revision.setChangedAt(LocalDateTime.now());
        revision.setUsername(username == null ? "anonymous" : username);
        revision.setOldData(oldData);

        if (entityManager != null) {
            entityManager.flush();
        }

        setOldData(null);
    }
}
