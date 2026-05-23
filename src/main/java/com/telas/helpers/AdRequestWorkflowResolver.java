package com.telas.helpers;

import com.telas.entities.Ad;
import com.telas.entities.AdRequest;
import com.telas.enums.AdRequestOrigin;
import com.telas.enums.AdRequestWorkflowStatus;
import com.telas.enums.AdValidationType;
import com.telas.enums.PartnerSubmissionMode;
import com.telas.enums.Role;

public final class AdRequestWorkflowResolver {

    private AdRequestWorkflowResolver() {
    }

    public static AdRequestWorkflowStatus resolve(AdRequest adRequest) {
        Ad ad = adRequest.getAd();
        if (PartnerSubmissionMode.PARTNER_FINISHED_CREATIVE.equals(adRequest.getSubmissionMode())
                && ad != null
                && AdValidationType.PENDING.equals(ad.getValidation())) {
            return AdRequestWorkflowStatus.AWAITING_ADMIN_DIRECT_APPROVAL;
        }
        if (ad != null && AdValidationType.PENDING.equals(ad.getValidation())) {
            if (AdRequestOrigin.PARTNER.equals(adRequest.getRequestOrigin())
                    || Role.PARTNER.equals(adRequest.getClient().getRole())) {
                return AdRequestWorkflowStatus.AWAITING_PARTNER_REVIEW;
            }
            return AdRequestWorkflowStatus.AWAITING_CLIENT_REVIEW;
        }
        if (ad != null && AdValidationType.REJECTED.equals(ad.getValidation())) {
            return AdRequestWorkflowStatus.REOPENED_AFTER_REJECTION;
        }
        return AdRequestWorkflowStatus.AWAITING_ADMIN_UPLOAD;
    }

    public static String adminActionLabel(AdRequestWorkflowStatus status) {
        return switch (status) {
            case AWAITING_ADMIN_UPLOAD -> "Upload creative for partner";
            case AWAITING_PARTNER_REVIEW -> "Waiting for partner review";
            case AWAITING_CLIENT_REVIEW -> "Waiting for client review";
            case AWAITING_ADMIN_DIRECT_APPROVAL -> "Approve finished creative";
            case REOPENED_AFTER_REJECTION -> "Re-upload after rejection";
        };
    }
}
