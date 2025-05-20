package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "subscriptions_flows")
@NoArgsConstructor
public class SubscriptionFlow implements Serializable {
  @Serial
  private static final long serialVersionUID = 1084934057135367842L;

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @Column(name = "step")
  private int step = 0;

  @JsonIgnore
  @OneToOne
  @JoinColumn(name = "client_id", referencedColumnName = "id")
  private Client client;

  public SubscriptionFlow(Client client) {
    this.client = client;
  }

  public void nextStep() {
    if (step < 2) {
      step++;
    }
  }
}
