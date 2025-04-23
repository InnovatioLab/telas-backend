package com.marketingproject.shared.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = -3671068621462712178L;

    @JsonIgnore
    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE", nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @JsonIgnore
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE", insertable = false)
    @LastModifiedDate
    private Instant updatedAt;

    @JsonIgnore
    @Column(name = "username_create", updatable = false)
    @CreatedBy
    private String usernameCreate;

    @JsonIgnore
    @Column(name = "username_update", insertable = false)
    @LastModifiedBy
    private String usernameUpdate;

    @PreUpdate
    public void preUpdate() {
        setUsernameUpdate(getUsernameUpdate());
    }

    @PrePersist
    public void prePersist() {
        setUsernameCreate(getUsernameCreate());
    }
}
