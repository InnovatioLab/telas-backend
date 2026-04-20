package com.telas.monitoring.entities;

import com.telas.entities.Box;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(schema = "monitoring", name = "smart_plug_accounts")
@NoArgsConstructor
public class SmartPlugAccountEntity {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id", nullable = false)
    private Box box;

    @Column(name = "vendor", nullable = false, length = 32)
    private String vendor;

    @Column(name = "account_email")
    private String accountEmail;

    @Column(name = "password_cipher", columnDefinition = "text")
    private String passwordCipher;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

