package com.telas.entities;

import com.telas.dtos.request.ClientAdRequestToAdminDto;
import com.telas.enums.Role;
import com.telas.shared.audit.BaseAudit;
import com.telas.shared.utils.ValidateDataUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;

import java.io.Serial;
import java.io.Serializable;
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

  @Column(name = "attachment_ids")
  private String attachmentIds;

  @Column(name = "phone")
  private String phone;

  @Column(name = "email")
  private String email;

  @Column(name = "active")
  private boolean isActive = true;

  @OneToOne(mappedBy = "adRequest")
  private Ad ad;

  @OneToOne
  @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
  private Client client;

  public AdRequest(ClientAdRequestToAdminDto request, Client client, List<Attachment> attachmentList) {
    this.client = client;
    message = request.getMessage();
    phone = request.getPhone();
    email = request.getEmail();

    if (!ValidateDataUtils.isNullOrEmpty(attachmentList)) {
      attachmentIds = attachmentList.stream()
              .map(Attachment::getId)
              .map(UUID::toString)
              .reduce((a, b) -> a + "," + b)
              .orElse("");
    }
  }

  public void closeRequest() {
    isActive = false;
  }

  public void openRequest() {
    isActive = true;
  }

  public void handleRefusal() {
    long refusedCount = ad.getRefusedAds().stream()
            .filter(refusedAd -> !Role.ADMIN.equals(refusedAd.getValidator().getRole()))
            .count();

    if (refusedCount <= 2) {
      openRequest();
    } else {
      closeRequest();
    }
  }
}
