package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telas.dtos.request.RefusedAttachmentRequestDto;
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
@Table(name = "refused_attachments")
@AuditTable("refused_attachments_aud")
@NoArgsConstructor
public class RefusedAttachment extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = 7994322371461512045L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "justification", nullable = false)
    private String justification;

    @Column(name = "description")
    private String description;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "attachment_id", referencedColumnName = "id", nullable = false)
    private AdvertisingAttachment attachment;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "validator_id", referencedColumnName = "id", nullable = false)
    private Client validator;

    public RefusedAttachment(RefusedAttachmentRequestDto request, AdvertisingAttachment attachment, Client validator) {
        justification = request.getJustification();
        description = request.getDescription();
        this.attachment = attachment;
        this.validator = validator;

    }


}
