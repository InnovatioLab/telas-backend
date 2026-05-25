package com.telas.helpers;

import com.telas.entities.Ad;
import com.telas.entities.Attachment;
import com.telas.services.BucketService;
import com.telas.shared.utils.AttachmentUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdMediaLinkFactory {

    private final BucketService bucketService;

    public String getLink(Ad ad) {
        return bucketService.getLink(AttachmentUtils.format(ad));
    }

    public String getLink(Attachment attachment) {
        return bucketService.getLink(AttachmentUtils.format(attachment));
    }

    public String getDownloadLink(Ad ad) {
        return bucketService.getDownloadLink(AttachmentUtils.format(ad), ad.getName());
    }

    public String getDownloadLink(Attachment attachment) {
        return bucketService.getDownloadLink(AttachmentUtils.format(attachment), attachment.getName());
    }
}
