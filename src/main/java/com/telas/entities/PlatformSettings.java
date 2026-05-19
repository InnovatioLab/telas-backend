package com.telas.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "platform_settings")
@NoArgsConstructor
public class PlatformSettings {

    @Id
    @Column(name = "id")
    private Short id = 1;

    @Column(name = "partner_slots_any_location_enabled", nullable = false)
    private boolean partnerSlotsAnyLocationEnabled = false;
}
