package com.telas.entities;

import com.telas.dtos.request.MonitorAdRequestDto;
import com.telas.shared.audit.BaseAudit;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "monitors_ads")
@AuditTable("monitors_ads_aud")
public class MonitorAd extends BaseAudit implements Serializable {
  @Serial
  private static final long serialVersionUID = 4636281925583628366L;

  @EmbeddedId
  private MonitorAdPK id = new MonitorAdPK();

  @Column(name = "order_index", nullable = false)
  private Integer orderIndex;

  public MonitorAd(MonitorAdRequestDto request, Monitor monitor, Ad ad) {
    id.setMonitor(monitor);
    id.setAd(ad);
    orderIndex = request.getOrderIndex();
  }

  public MonitorAd(Monitor monitor, Ad ad) {
    id.setMonitor(monitor);
    id.setAd(ad);
    orderIndex = monitor.getAds().size() + 1;
  }

  public Monitor getMonitor() {
    return id.getMonitor();
  }

  public void setMonitor(Monitor monitor) {
    id.setMonitor(monitor);
  }

  public Ad getAd() {
    return id.getAd();
  }

  public void setAd(Ad ad) {
    id.setAd(ad);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MonitorAd monitorAd = (MonitorAd) o;
    return Objects.equals(id, monitorAd.id);
  }
}