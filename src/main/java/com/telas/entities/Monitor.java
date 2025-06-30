package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.enums.MonitorType;
import com.telas.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.NotAudited;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "monitors")
@AuditTable("monitors_aud")
@NoArgsConstructor
public class Monitor extends BaseAudit implements Serializable {
  @Serial
  private static final long serialVersionUID = 1084934057135367842L;

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @Column(name = "fl_active", nullable = false)
  private boolean active = true;

  @Column(name = "type", nullable = false)
  @Enumerated(EnumType.STRING)
  private MonitorType type = MonitorType.BASIC;

  @Column(name = "location_description")
  private String locationDescription;

  @Column(name = "size_in_inches", precision = 5, scale = 2, nullable = false)
  private BigDecimal size = BigDecimal.valueOf(0.00);

  @Column(name = "product_id", nullable = false)
  private String productId;

  @Column(name = "max_blocks")
  private Integer maxBlocks = 12;

  @ManyToOne
  @JoinColumn(name = "address_id", referencedColumnName = "id", nullable = false)
  private Address address;

  @JsonIgnore
  @OneToMany(mappedBy = "id.monitor", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<MonitorAd> monitorAds = new HashSet<>();

  @ManyToMany
  @JoinTable(
          name = "clients_monitors",
          joinColumns = @JoinColumn(name = "monitor_id"),
          inverseJoinColumns = @JoinColumn(name = "client_id")
  )
  private Set<Client> clients = new HashSet<Client>();

  @JsonIgnore
  @NotAudited
  @ManyToOne
  @JoinColumn(name = "box_id", referencedColumnName = "id")
  private Box box;

  public Monitor(MonitorRequestDto request, Address address) {
    productId = request.getProductId();
    type = request.getType();
    size = request.getSize();
    locationDescription = request.getLocationDescription();
    maxBlocks = request.getMaxBlocks();
    this.address = address;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Monitor monitor = (Monitor) o;
    return Objects.equals(getId(), monitor.getId());
  }

  public List<Ad> getAds() {
    return monitorAds.stream()
            .map(MonitorAd::getAd)
            .toList();
  }

  public boolean hasAvailableBlocks(int blocksWanted) {
    return (monitorAds.size() + blocksWanted) <= maxBlocks;
  }

  public boolean clientAlreadyHasAd(Client client) {
    return getAds().stream()
            .anyMatch(ad -> ad.getClient() != null && ad.getClient().getId().equals(client.getId()));
  }
}
