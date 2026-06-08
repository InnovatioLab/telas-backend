package com.telas.services.impl;

import com.telas.entities.Ad;
import com.telas.entities.AdRequest;
import com.telas.entities.Attachment;
import com.telas.entities.Client;
import com.telas.repositories.AdRepository;
import com.telas.repositories.AttachmentRepository;
import com.telas.repositories.MonitorAdRepository;
import com.telas.services.BucketService;
import com.telas.shared.utils.AttachmentUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnusedSingleAdDeletionService {

    private static final Logger log = LoggerFactory.getLogger(UnusedSingleAdDeletionService.class);

    private final AdRepository adRepository;
    private final AttachmentRepository attachmentRepository;
    private final MonitorAdRepository monitorAdRepository;
    private final BucketService bucketService;

    @Autowired
    @Lazy
    private UnusedSingleAdDeletionService self;

    @Async
    public void deleteAdAsync(UUID adId) {
        try {
            self.deleteAdInNewTransaction(adId);
        } catch (Exception ex) {
            log.error("Async ad deletion failed adId={}: {}", adId, ex.getMessage(), ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAdInNewTransaction(UUID adId) {
        Ad ad = adRepository.findById(adId).orElse(null);
        if (ad == null) {
            return;
        }
        Client client = ad.getClient();
        if (client != null) {
            client.getAds().remove(ad);
        }
        Set<Attachment> attachments = new HashSet<>(ad.getAttachments());
        for (Attachment att : attachments) {
            try {
                bucketService.deleteAttachment(AttachmentUtils.format(att));
            } catch (Exception ex) {
                log.warn(
                        "Could not delete attachment object from bucket id={}: {}",
                        att.getId(),
                        ex.getMessage());
            }
            ad.getAttachments().remove(att);
            attachmentRepository.delete(att);
        }
        try {
            bucketService.deleteAttachment(AttachmentUtils.format(ad));
        } catch (Exception ex) {
            log.warn("Could not delete main ad file from bucket adId={}: {}", adId, ex.getMessage());
        }
        AdRequest req = ad.getAdRequest();
        if (req != null) {
            req.setAd(null);
            ad.setAdRequest(null);
        }
        monitorAdRepository.deleteByAdId(adId);
        adRepository.delete(ad);
    }
}
