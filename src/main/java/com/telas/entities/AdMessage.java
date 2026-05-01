package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telas.enums.Role;
import com.telas.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ad_messages")
@AuditTable("ad_messages_aud")
@NoArgsConstructor
public class AdMessage extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = -3946406094130929814L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false, length = 15)
    private Role senderRole;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", referencedColumnName = "id", nullable = false)
    private Ad ad;

    public AdMessage(Ad ad, Role senderRole, String message) {
        this.ad = ad;
        this.senderRole = senderRole;
        this.message = message;
    }
}

