package com.marketingproject.entities;

import com.marketingproject.dtos.request.ContactRequestDto;
import com.marketingproject.enums.ContactPreference;
import com.marketingproject.shared.audit.BaseAudit;
import com.marketingproject.shared.utils.ValidateDataUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
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

    @Column(name = "contact_preference", columnDefinition = "contact_preference", nullable = false)
    @Enumerated(EnumType.STRING)
    private ContactPreference contactPreference;

    public Contact(ContactRequestDto request) {
        email = request.getEmail();
        phone = request.getPhone();
        contactPreference = request.getContactPreference();
    }

    public boolean validateChangesUpdateCode(ContactRequestDto request) {
        if (!contactPreference.equals(request.getContactPreference())) {
            return true;
        }
        return ContactPreference.EMAIL.equals(request.getContactPreference())
                ? !Objects.equals(email, request.getEmail())
                : !Objects.equals(phone, request.getPhone());
    }

    public void applyChangesUpdateCode(ContactRequestDto request) {
        contactPreference = request.getContactPreference();
        phone = ValidateDataUtils.isNullOrEmptyString(request.getPhone()) ? phone : request.getPhone();
        email = ValidateDataUtils.isNullOrEmptyString(request.getEmail()) ? email : request.getEmail();
    }

    public void update(ContactRequestDto contact) {
        contactPreference = contact.getContactPreference();
        if (ContactPreference.EMAIL.equals(contactPreference)) {
            email = ValidateDataUtils.isNullOrEmptyString(contact.getEmail()) ? email : contact.getEmail();
        } else {
            phone = ValidateDataUtils.isNullOrEmptyString(contact.getPhone()) ? phone : contact.getPhone();
        }
    }
}
