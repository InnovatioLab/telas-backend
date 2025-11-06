package com.telas.entities;

import com.telas.dtos.request.ContactRequestDto;
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
@Table(name = "contacts")
@AuditTable("contacts_aud")
@NoArgsConstructor
public class Contact extends BaseAudit implements Serializable {
  @Serial
  private static final long serialVersionUID = 1084934057135367842L;

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "phone")
  private String phone;

  public Contact(ContactRequestDto request) {
    email = request.getEmail();
    phone = request.getPhone();
  }

  public void update(ContactRequestDto request) {
    phone = request.getPhone();
  }
}
