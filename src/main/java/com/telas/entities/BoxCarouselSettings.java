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
@Table(name = "box_carousel_settings")
@NoArgsConstructor
public class BoxCarouselSettings {

    @Id
    @Column(name = "id")
    private Short id = 1;

    @Column(name = "display_duration_ms", nullable = false)
    private int displayDurationMs = 5000;

    @Column(name = "transition_duration_ms", nullable = false)
    private int transitionDurationMs = 2000;

    @Column(name = "total_slots", nullable = false)
    private short totalSlots = 15;
}
