package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "boxes")
@NoArgsConstructor
public class Box implements Serializable {
  @Serial
  private static final long serialVersionUID = 1084934057135367842L;

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @Column(name = "fl_active", nullable = false)
  private boolean active = true;

  @OneToOne
  @JoinColumn(name = "ip_id", referencedColumnName = "id", nullable = false)
  private Ip ip;

  @JsonIgnore
  @Column(name = "created_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE", nullable = false, updatable = false)
  @CreatedDate
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "box", cascade = CascadeType.ALL)
  private List<Monitor> monitors = new ArrayList<>();

  public Box(Ip ip, List<Monitor> monitors) {
    this.ip = ip;
    this.monitors.addAll(monitors);
  }
}
