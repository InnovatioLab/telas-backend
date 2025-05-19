package com.telas.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telas.dtos.request.CartItemRequestDto;
import com.telas.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "carts_items")
@AuditTable("carts_items_aud")
@NoArgsConstructor
public class CartItem extends BaseAudit implements Serializable {
  @Serial
  private static final long serialVersionUID = 1084934057135367842L;

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "cart_id", referencedColumnName = "id", nullable = false)
  private Cart cart;

  @ManyToOne
  @JoinColumn(name = "monitor_id", referencedColumnName = "id", nullable = false)
  private Monitor monitor;

  @Column(name = "block_quantity", nullable = false)
  private Integer blockQuantity = 1;

  public CartItem(Cart cart, Monitor monitor, CartItemRequestDto request) {
    this.cart = cart;
    this.monitor = monitor;
    blockQuantity = request.getBlockQuantity();
  }

  public String toStringMapper() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    return objectMapper.writeValueAsString(this);
  }
}
