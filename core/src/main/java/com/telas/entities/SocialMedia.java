package com.telas.entities;

import com.telas.dtos.request.SocialMediaRequestDto;
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
@Table(name = "social_medias")
@AuditTable("social_medias_aud")
@NoArgsConstructor
public class SocialMedia extends BaseAudit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1084934057135367842L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "instagram_url", columnDefinition = "TEXT")
    private String instagramUrl;

    @Column(name = "facebook_url", columnDefinition = "TEXT")
    private String facebookUrl;

    @Column(name = "linkedin_url", columnDefinition = "TEXT")
    private String linkedinUrl;

    @Column(name = "x_url", columnDefinition = "TEXT")
    private String xUrl;

    @Column(name = "tiktok_url", columnDefinition = "TEXT")
    private String tiktokUrl;

    public SocialMedia(SocialMediaRequestDto socialMediaRequestDto) {
        instagramUrl = socialMediaRequestDto.getInstagramUrl();
        facebookUrl = socialMediaRequestDto.getFacebookUrl();
        linkedinUrl = socialMediaRequestDto.getLinkedinUrl();
        xUrl = socialMediaRequestDto.getXUrl();
    }

    public void update(SocialMediaRequestDto socialMedia) {
        instagramUrl = socialMedia.getInstagramUrl();
        facebookUrl = socialMedia.getFacebookUrl();
        linkedinUrl = socialMedia.getLinkedinUrl();
        xUrl = socialMedia.getXUrl();
    }
}
