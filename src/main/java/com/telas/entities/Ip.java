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
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ips")
@NoArgsConstructor
public class Ip implements Serializable {
  @Serial
  private static final long serialVersionUID = 1084934057135367842L;

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @Column(name = "ip_address", nullable = false)
  private String ipAddress;

  @JsonIgnore
  @Column(name = "created_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE", nullable = false, updatable = false)
  @CreatedDate
  private Instant createdAt = Instant.now();

  public Ip(String ipAddress) {
    this.ipAddress = ipAddress;
  }
}
