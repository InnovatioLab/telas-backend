package com.telas.shared.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit")
@RevisionEntity(CustomRevisionListener.class)
@Getter
@Setter
@ToString(callSuper = true)
public class Audit {

    @Id
    @RevisionNumber
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_seq")
    @SequenceGenerator(name = "audit_seq", sequenceName = "public.audit_seq", allocationSize = 1)
    @Column(name = "audit_id", nullable = false)
    private Long id;

    @RevisionTimestamp
    @Column(name = "timestamp_number")
    @JsonIgnore
    private long timestamp;

    @Column(name = "changed_at")
    @JsonIgnore
    private LocalDateTime changedAt;

    @Column(name = "username")
    private String username;

    @Column(name = "old_data")
    private String oldData;

}
