package com.telas.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telas.dtos.request.AdRequestDto;
import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.enums.AdValidationType;
import com.telas.shared.audit.BaseAudit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.NotAudited;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ads")
@AuditTable("ads_aud")
@NoArgsConstructor
public class Ad extends BaseAudit implements Serializable {
  @Serial
  private static final long serialVersionUID = 1084934057135367842L;

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "mime_type", nullable = false, length = 5)
  private String type;

  @Column(name = "validation", nullable = false)
  @Enumerated(EnumType.STRING)
  private AdValidationType validation = AdValidationType.PENDING;

  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
  private Client client;

  @JsonIgnore
  @NotAudited
  @OneToOne
  @JoinColumn(name = "ad_request_id", referencedColumnName = "id")
  private AdRequest adRequest;

  @JsonIgnore
  @NotAudited
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
          name = "ads_attachments",
          joinColumns = @JoinColumn(name = "ad_id"),
          inverseJoinColumns = @JoinColumn(name = "attachment_id")
  )
  private Set<Attachment> attachments = new HashSet<>();

  @JsonIgnore
  @OneToMany(mappedBy = "id.ad")
  private Set<MonitorAd> monitorAds = new HashSet<>();

  public Ad(AttachmentRequestDto request, Client client) {
    name = request.getName();
    type = request.getType();
    this.client = client;
  }

  public Ad(AdRequestDto request, Client client, AdRequest adRequest) {
    name = request.getName();
    type = request.getType();
    this.client = client;
    this.adRequest = adRequest;
  }

  public String toStringMapper() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    return objectMapper.writeValueAsString(this);
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
    Ad that = (Ad) o;
    return Objects.equals(getId(), that.getId());
  }
}
