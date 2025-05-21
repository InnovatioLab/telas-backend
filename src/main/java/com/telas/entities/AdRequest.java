package com.telas.entities;

import com.telas.dtos.request.ClientAdRequestToAdminDto;
import com.telas.shared.audit.BaseAudit;
import com.telas.shared.constants.SharedConstants;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ad_requests")
@AuditTable("ad_requests_aud")
@NoArgsConstructor
public class AdRequest extends BaseAudit implements Serializable {
  @Serial
  private static final long serialVersionUID = 1084934057135367842L;

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @Column(name = "message", nullable = false)
  private String message;

  @Column(name = "attachment_ids", nullable = false)
  private String attachmentIds;

  @Column(name = "phone")
  private String phone;

  @Column(name = "email")
  private String email;

  @Column(name = "active")
  private boolean isActive = true;

  @Column(name = "refusal_count", nullable = false)
  private int refusalCount = 0;

  @OneToOne(mappedBy = "adRequest", cascade = CascadeType.ALL)
  private Ad ad;

  @OneToMany(mappedBy = "adRequest", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RefusedAd> refusedAds = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  public AdRequest(ClientAdRequestToAdminDto request, Client client, List<Attachment> attachmentList) {
    this.client = client;
    message = request.getMessage();
    phone = request.getPhone();
    email = request.getEmail();
    attachmentIds = attachmentList.stream().map(Attachment::getId).map(UUID::toString).reduce((a, b) -> a + "," + b).orElse("");
  }

  public void closeRequest() {
    isActive = false;
  }

  public void openRequest() {
    isActive = true;
  }

  public void incrementRefusalCount() {
    if (refusalCount <= 2) {
      refusalCount++;
      openRequest();
    } else {
      closeRequest();
    }
  }

  public boolean canBeRefused() {
    return refusalCount < SharedConstants.MAX_ADS_VALIDATION;
  }
}
