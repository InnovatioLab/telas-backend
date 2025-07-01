package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "wishlist")
public class Wishlist implements Serializable {
  @Serial
  private static final long serialVersionUID = 7146809860790952328L;

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @JsonIgnore
  @OneToOne
  @JoinColumn(name = "client_id", unique = true, nullable = false)
  private Client client;

  @ManyToMany
  @JoinTable(
          name = "wishlist_monitors",
          joinColumns = @JoinColumn(name = "wishlist_id"),
          inverseJoinColumns = @JoinColumn(name = "monitor_id")
  )
  private Set<Monitor> monitors = new HashSet<>();

  public Wishlist(Client client) {
    this.client = client;
  }
}