package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telas.dtos.request.OwnerRequestDto;
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
@Table(name = "owners")
@AuditTable("owners_aud")
@NoArgsConstructor
public class Owner extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1084934057135367842L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "identification_number", nullable = false, unique = true)
    private String identificationNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @JsonIgnore
    @OneToOne(mappedBy = "owner")
    private Client client;

    public Owner(OwnerRequestDto request, Client client) {
        identificationNumber = request.getIdentificationNumber();
        firstName = request.getFirstName();
        lastName = request.getLastName();
        email = request.getEmail();
        phone = request.getPhone();
        this.client = client;
    }

    public void update(OwnerRequestDto owner) {
        identificationNumber = owner.getIdentificationNumber();
        firstName = owner.getFirstName();
        lastName = owner.getLastName();
        email = owner.getEmail();
        phone = owner.getPhone();
    }
}
