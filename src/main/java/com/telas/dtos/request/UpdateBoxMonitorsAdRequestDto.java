package com.telas.dtos.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.entities.Ad;
import com.telas.entities.MonitorAd;
import com.telas.entities.SubscriptionMonitor;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.utils.TrimStringDeserializer;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBoxMonitorsAdRequestDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -3963846843873646628L;

  private Integer blockQuantity = 1;

  private Integer orderIndex;

  @JsonDeserialize(using = TrimStringDeserializer.class)
  private String fileName;

  @JsonDeserialize(using = TrimStringDeserializer.class)
  private String link;

    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String baseUrl;

    public UpdateBoxMonitorsAdRequestDto(Ad ad, MonitorAd monitorAd, SubscriptionMonitor subscriptionMonitor, String link) {
        this.blockQuantity = subscriptionMonitor.getSlotsQuantity();
        this.orderIndex = monitorAd.getOrderIndex();
        this.fileName = ad.getName();
        this.link = link;
        this.baseUrl = String.format("http://%s:8081/", monitorAd.getMonitor().getBox().getBoxAddress().getIp());
    }

    public UpdateBoxMonitorsAdRequestDto(Ad ad, MonitorAd monitorAd, String link) {
        this.link = link;
        this.orderIndex = monitorAd.getOrderIndex();
        this.fileName = ad.getName();
        this.baseUrl = String.format("http://%s:8081/", monitorAd.getMonitor().getBox().getBoxAddress().getIp());
    }
}