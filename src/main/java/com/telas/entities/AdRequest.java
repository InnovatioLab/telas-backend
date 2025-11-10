package com.telas.entities;

import com.telas.dtos.request.ClientAdRequestToAdminDto;
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
@NoArgsConstructor
public class AdRequest extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1084934057135367842L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "slogan")
    private String slogan;

    @Column(name = "brand_guideline_url", columnDefinition = "TEXT")
    private String brandGuidelineUrl;

    @Column(name = "attachment_ids")
    private String attachmentIds;

    @Column(name = "active")
    private boolean isActive = true;

    @OneToOne(mappedBy = "adRequest")
    private Ad ad;

    @OneToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false)
    private Client client;

    public AdRequest(ClientAdRequestToAdminDto request, Client client, List<Attachment> attachmentList) {
        this.client = client;
        this.slogan = request.getSlogan();
        this.brandGuidelineUrl = request.getBrandGuidelineUrl();

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
        if (ad.getRefusedAds().size() <= 2) {
            openRequest();
        } else {
            closeRequest();
        }
    }
}
