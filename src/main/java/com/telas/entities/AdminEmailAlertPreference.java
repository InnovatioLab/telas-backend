package com.telas.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "admin_email_alert_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "alert_category"}))
@NoArgsConstructor
public class AdminEmailAlertPreference {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "alert_category", nullable = false, length = 64)
    private String alertCategory;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
