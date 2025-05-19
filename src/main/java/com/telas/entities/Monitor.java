package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.enums.MonitorType;
import com.telas.shared.audit.BaseAudit;
import com.telas.shared.utils.ValidateDataUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.IntStream;

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

  @Column(name = "max_blocks")
  private Integer maxBlocks = 12;

  @ManyToOne
  @JoinColumn(name = "address_id", referencedColumnName = "id", nullable = false)
  private Address address;

  @OneToOne
  @JoinColumn(name = "partner_id", referencedColumnName = "id")
  private Client partner;

  @JsonIgnore
  @OneToMany(mappedBy = "id.monitor")
  private Set<MonitorAd> monitorAds = new HashSet<>();

  @ManyToMany
  @JoinTable(
          name = "clients_monitors",
          joinColumns = @JoinColumn(name = "monitor_id"),
          inverseJoinColumns = @JoinColumn(name = "client_id")
  )
  private Set<Client> clients = new HashSet<Client>();

  public Monitor(MonitorRequestDto request, Client partner, Address address) {
    this(request, partner, address, Set.of(), List.of());
  }

  public Monitor(MonitorRequestDto request, Client partner, Address address, Set<Client> clients, List<Ad> advertisingAttachmentsList) {
    type = request.getType();
    size = request.getSize();
    locationDescription = request.getLocationDescription();
    maxBlocks = request.getMaxBlocks();
    this.address = address;
    this.partner = partner;

    if (!ValidateDataUtils.isNullOrEmpty(request.getAds())) {
      IntStream.range(0, advertisingAttachmentsList.size()).forEach(index -> {
        Ad ad = advertisingAttachmentsList.get(index);
        MonitorAd monitorAd = new MonitorAd(request.getAds().get(index), this, ad);
        monitorAds.add(monitorAd);
      });
    }

    this.clients.addAll(clients);
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

  public List<Ad> getAdvertisingAttachments() {
    return monitorAds.stream()
            .map(MonitorAd::getAd)
            .toList();
  }

  public boolean hasAvailableBlocks(int blocksWanted) {
    int activeBlocks = monitorAds.stream()
            .mapToInt(MonitorAd::getBlockQuantity)
            .sum();

    return (activeBlocks + blocksWanted) <= maxBlocks;
  }

  public String getPartnerBusinessName() {
    return partner.getBusinessName();
  }

  public String toStringMapper() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    return objectMapper.writeValueAsString(this);
  }
}
