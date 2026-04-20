package com.telas.monitoring.entities;

import com.telas.entities.Box;
import com.telas.entities.Monitor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(schema = "monitoring", name = "smart_plugs")
@NoArgsConstructor
public class SmartPlugEntity {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "mac_address", nullable = false, length = 32)
    private String macAddress;

    @Column(name = "vendor", nullable = false, length = 32)
    private String vendor;

    @Column(name = "model", length = 128)
    private String model;

    @Column(name = "display_name")
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monitor_id", nullable = true)
    private Monitor monitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id", nullable = true)
    private Box box;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "smart_plug_account_id", nullable = true)
    private SmartPlugAccountEntity smartPlugAccount;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "last_seen_ip", length = 45)
    private String lastSeenIp;

    @Column(name = "account_email")
    private String accountEmail;

    @Column(name = "password_cipher", columnDefinition = "text")
    private String passwordCipher;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
