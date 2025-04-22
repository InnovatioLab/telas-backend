package com.marketingproject.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.marketingproject.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "advertising_attachments")
@NoArgsConstructor
public class AdvertisingAttachment extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1084934057135367842L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "mime_type", nullable = false, length = 5)
    private String type;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    private Client client;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "advertising_attachments_attachments",
            joinColumns = @JoinColumn(name = "advertising_attachment_id"),
            inverseJoinColumns = @JoinColumn(name = "attachment_id")
    )
    private Set<Attachment> attachments = new HashSet<>();
}
