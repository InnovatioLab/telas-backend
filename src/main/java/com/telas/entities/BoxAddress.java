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
@Table(name = "box_address")
@NoArgsConstructor
public class BoxAddress implements Serializable {
  @Serial
  private static final long serialVersionUID = 1084934057135367842L;

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @Column(name = "ip", nullable = false, unique = true)
  private String ip;

  @Column(name = "mac", nullable = false, unique = true)
  private String mac;

  @Column(name = "dns")
  private String dns;

  @JsonIgnore
  @OneToOne(mappedBy = "boxAddress", cascade = CascadeType.ALL)
  private Box box;

  @JsonIgnore
  @Column(name = "created_at", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE", nullable = false, updatable = false)
  @CreatedDate
  private Instant createdAt = Instant.now();
}
