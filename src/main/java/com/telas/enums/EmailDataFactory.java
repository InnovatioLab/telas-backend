package com.telas.enums;

import com.telas.dtos.EmailDataDto;
import com.telas.shared.constants.SharedConstants;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

public final class EmailDataFactory {

    /** Sentinel returned by entries that produce no email. Callers check {@code != null}. */
    public static final EmailDataDto NO_EMAIL = null;

    private EmailDataFactory() {}

    public static boolean isPartnerActor(Map<String, String> params) {
        return "partner".equals(params.get("actorType"));
    }

    public static EmailDataDto forSubscription(String subject, String template, Map<String, String> params, String startDate, String endDate) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(subject);
        emailData.setTemplate(template);
        emailData.getParams().put("name", params.get("name"));
        emailData.getParams().put("locations", params.get("locations"));
        emailData.getParams().put("link", params.get("link"));
        emailData.getParams().put("startDate", startDate);
        emailData.getParams().put("endDate", ObjectUtils.isEmpty(endDate) ? "" : endDate);
        return emailData;
    }

    public static EmailDataDto forCountdownExpiry(String subject, String template, Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(subject);
        emailData.setTemplate(template);
        emailData.getParams().put("name", params.get("name"));
        emailData.getParams().put("link", params.get("link"));
        emailData.getParams().put("endDate", ObjectUtils.isEmpty(params.get("endDate")) ? "" : params.get("endDate"));
        emailData.getParams().put("daysRemaining", ObjectUtils.isEmpty(params.get("daysRemaining")) ? "" : params.get("daysRemaining"));
        return emailData;
    }

    public static EmailDataDto forPenultimateExpiry(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_SUBSCRIPTION_EXPIRING_PENULTIMATE);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_SUBSCRIPTION_EXPIRING_PENULTIMATE);
        emailData.getParams().put("name", params.get("name"));
        emailData.getParams().put("link", params.get("link"));
        emailData.getParams().put("endDate", ObjectUtils.isEmpty(params.get("endDate")) ? "" : params.get("endDate"));
        return emailData;
    }

    public static EmailDataDto forClientAdReceived(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_CLIENT_AD_RECEIVED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_CLIENT_AD_RECEIVED);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("name", params.getOrDefault("name", ""));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        return emailData;
    }

    public static EmailDataDto forAdResubmittedClient(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_AD_RESUBMITTED_CLIENT);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_AD_RESUBMITTED_CLIENT);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("name", params.getOrDefault("name", ""));
        emailData.getParams().put("adName", params.getOrDefault("adName", "Ad"));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        return emailData;
    }

    public static EmailDataDto forAdminAdResubmitted(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_ADMIN_AD_RESUBMITTED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_AD_RESUBMITTED);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("clientName", params.getOrDefault("clientName", ""));
        emailData.getParams().put("adName", params.getOrDefault("adName", ""));
        emailData.getParams().put("adminName", params.getOrDefault("adminName", ""));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        return emailData;
    }

    public static EmailDataDto forClientAdRejectedAdmin(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        boolean partner = isPartnerActor(params);
        emailData.setSubject(partner
                ? SharedConstants.EMAIL_SUBJECT_CLIENT_AD_REJECTED_PARTNER
                : SharedConstants.EMAIL_SUBJECT_CLIENT_AD_REJECTED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_CLIENT_AD_REJECTED);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("clientName", params.getOrDefault("name", ""));
        emailData.getParams().put("adName", params.getOrDefault("adName", "Ad"));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        emailData.getParams().put("justification", params.getOrDefault("justification", ""));
        emailData.getParams().put("description", params.getOrDefault("description", ""));
        emailData.getParams().put("actorType", partner ? "partner" : "customer");
        return emailData;
    }

    public static EmailDataDto forClientAdRejectionConfirmed(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_CLIENT_AD_REJECTION_CONFIRMED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_CLIENT_AD_REJECTION_CONFIRMED);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("name", params.getOrDefault("name", ""));
        emailData.getParams().put("adName", params.getOrDefault("adName", "Ad"));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        return emailData;
    }

    public static EmailDataDto forClientAdApproved(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_CLIENT_AD_APPROVED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_CLIENT_AD_APPROVED);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("name", params.getOrDefault("name", ""));
        emailData.getParams().put("adName", params.getOrDefault("adName", "Ad"));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        emailData.getParams().put("partner", params.getOrDefault("partner", "false"));
        emailData.getParams().put("liveOnScreen", params.getOrDefault("liveOnScreen", "false"));
        emailData.getParams().put("linkLabel", params.getOrDefault("linkLabel", "My Telas — Ads"));
        return emailData;
    }

    public static EmailDataDto forAdminClientAdApproved(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        boolean partner = isPartnerActor(params);
        emailData.setSubject(partner
                ? SharedConstants.EMAIL_SUBJECT_ADMIN_PARTNER_AD_APPROVED
                : SharedConstants.EMAIL_SUBJECT_ADMIN_CLIENT_AD_APPROVED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_CLIENT_AD_APPROVED);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("clientName", params.getOrDefault("clientName", ""));
        emailData.getParams().put("adName", params.getOrDefault("adName", "Ad"));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        emailData.getParams().put("actorType", partner ? "partner" : "customer");
        return emailData;
    }

    public static EmailDataDto forAdminClientFirstAttachmentsUploaded(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        boolean partner = isPartnerActor(params);
        emailData.setSubject(partner
                ? SharedConstants.EMAIL_SUBJECT_ADMIN_PARTNER_FIRST_ATTACHMENTS_UPLOADED
                : SharedConstants.EMAIL_SUBJECT_ADMIN_CLIENT_FIRST_ATTACHMENTS_UPLOADED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_CLIENT_FIRST_ATTACHMENTS_UPLOADED);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("clientName", params.getOrDefault("clientName", ""));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        emailData.getParams().put("actorType", partner ? "partner" : "customer");
        return emailData;
    }

    public static EmailDataDto forClientFirstAttachmentsUploadedAck(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_CLIENT_FIRST_ATTACHMENTS_UPLOADED_ACK);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_CLIENT_FIRST_ATTACHMENTS_UPLOADED_ACK);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("name", params.getOrDefault("name", ""));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        emailData.getParams().put("partner", params.getOrDefault("partner", "false"));
        emailData.getParams().put("linkLabel", params.getOrDefault("linkLabel", "My Telas"));
        return emailData;
    }

    public static EmailDataDto forClientPartnerSubmissionAck(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_CLIENT_PARTNER_SUBMISSION_ACK);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_CLIENT_PARTNER_SUBMISSION_ACK);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("name", params.getOrDefault("name", ""));
        emailData.getParams().put("submissionType", params.getOrDefault("submissionType", ""));
        emailData.getParams().put("submissionKind", params.getOrDefault("submissionKind", ""));
        emailData.getParams().put("monitorLabel", params.getOrDefault("monitorLabel", ""));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        emailData.getParams().put("linkLabel", params.getOrDefault("linkLabel", "My screens"));
        return emailData;
    }

    public static EmailDataDto forClientAdOnAir(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_CLIENT_AD_ON_AIR);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_CLIENT_AD_ON_AIR);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("name", params.getOrDefault("name", ""));
        emailData.getParams().put("adName", params.getOrDefault("adName", "Ad"));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        emailData.getParams().put("partner", params.getOrDefault("partner", "false"));
        return emailData;
    }

    public static EmailDataDto forAdminAdOnAir(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_ADMIN_AD_ON_AIR);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_AD_ON_AIR);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("clientName", params.getOrDefault("clientName", ""));
        emailData.getParams().put("adName", params.getOrDefault("adName", "Ad"));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        emailData.getParams().put("actorType", params.getOrDefault("actorType", "customer"));
        return emailData;
    }

    public static EmailDataDto forClientAdDeployedToBox(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_CLIENT_AD_DEPLOYED_TO_BOX);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_CLIENT_AD_DEPLOYED_TO_BOX);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("name", params.getOrDefault("name", ""));
        emailData.getParams().put("adName", params.getOrDefault("adName", "Ad"));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        emailData.getParams().put("partner", params.getOrDefault("partner", "false"));
        emailData.getParams().put("monitorsSummary", params.getOrDefault("monitorsSummary", ""));
        emailData.getParams().put("subscriptionEndsAt", params.getOrDefault("subscriptionEndsAt", ""));
        return emailData;
    }

    public static EmailDataDto forAdminClientAdDeployedToBox(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        boolean partner = isPartnerActor(params);
        emailData.setSubject(partner
                ? SharedConstants.EMAIL_SUBJECT_ADMIN_PARTNER_AD_DEPLOYED_TO_BOX
                : SharedConstants.EMAIL_SUBJECT_ADMIN_CLIENT_AD_DEPLOYED_TO_BOX);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_CLIENT_AD_DEPLOYED_TO_BOX);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("clientName", params.getOrDefault("clientName", ""));
        emailData.getParams().put("adName", params.getOrDefault("adName", "Ad"));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        emailData.getParams().put("monitorsSummary", params.getOrDefault("monitorsSummary", ""));
        emailData.getParams().put("subscriptionEndsAt", params.getOrDefault("subscriptionEndsAt", ""));
        emailData.getParams().put("actorType", partner ? "partner" : "customer");
        return emailData;
    }

    public static EmailDataDto forMonitorWishlistAvailable(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_MONITOR_WISHLIST_AVAILABLE);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_MONITOR_WISHLIST_AVAILABLE);
        emailData.setParams(new HashMap<>());
        String name = params.getOrDefault("name", params.getOrDefault("clientName", ""));
        emailData.getParams().put("name", name);
        emailData.getParams().put("monitorsAddress", params.getOrDefault("monitorsAddress", ""));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        return emailData;
    }

    public static EmailDataDto forAdminNewClientRegistered(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_ADMIN_NEW_CLIENT_REGISTERED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_NEW_CLIENT_REGISTERED);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("businessName", ObjectUtils.isEmpty(params.get("businessName")) ? "" : params.get("businessName"));
        emailData.getParams().put("contactEmail", ObjectUtils.isEmpty(params.get("contactEmail")) ? "" : params.get("contactEmail"));
        emailData.getParams().put("clientId", ObjectUtils.isEmpty(params.get("clientId")) ? "" : params.get("clientId"));
        emailData.getParams().put("link", ObjectUtils.isEmpty(params.get("link")) ? "" : params.get("link"));
        return emailData;
    }

    public static EmailDataDto forAdminNewPurchase(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_ADMIN_NEW_PURCHASE);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_NEW_PURCHASE);
        emailData.getParams().put("buyerName", ObjectUtils.isEmpty(params.get("buyerName")) ? "" : params.get("buyerName"));
        emailData.getParams().put("monitorsDetailHtml", ObjectUtils.isEmpty(params.get("monitorsDetailHtml")) ? "" : params.get("monitorsDetailHtml"));
        emailData.getParams().put("attachmentListHtml", ObjectUtils.isEmpty(params.get("attachmentListHtml")) ? "" : params.get("attachmentListHtml"));
        emailData.getParams().put("veiculationSummary", ObjectUtils.isEmpty(params.get("veiculationSummary")) ? "" : params.get("veiculationSummary"));
        emailData.getParams().put("subscriptionId", ObjectUtils.isEmpty(params.get("subscriptionId")) ? "" : params.get("subscriptionId"));
        return emailData;
    }

    public static EmailDataDto forPartnerAdRemovalRequested(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_PARTNER_AD_REMOVAL_REQUESTED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_PARTNER_AD_REMOVAL_REQUESTED);
        emailData.setParams(new HashMap<>(params));
        return emailData;
    }

    public static EmailDataDto forPartnerAdRemovalConfirmed(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_PARTNER_AD_REMOVAL_CONFIRMED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_PARTNER_AD_REMOVAL_CONFIRMED);
        emailData.setParams(new HashMap<>(params));
        return emailData;
    }

    public static EmailDataDto forHostReboot(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setParams(new HashMap<>());
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_HOST_REBOOT);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_HOST_REBOOT);
        emailData.getParams().put("boxIp", params.getOrDefault("boxIp", ""));
        emailData.getParams().put("incidentType", params.getOrDefault("incidentType", ""));
        emailData.getParams().put("severity", params.getOrDefault("severity", ""));
        emailData.getParams().put("uptimeDropSeconds", params.getOrDefault("uptimeDropSeconds", ""));
        emailData.getParams().put("notifiedAt", params.getOrDefault("notifiedAt", ""));
        return emailData;
    }

    public static EmailDataDto forBoxStatusUpdated(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setParams(new HashMap<>());
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_BOX_STATUS_UPDATED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_BOX_STATUS_UPDATED);
        emailData.getParams().put("ip", params.get("ip"));
        emailData.getParams().put("statusLabel", params.get("statusLabel"));
        emailData.getParams().put("monitorAddresses", params.get("monitorAddresses"));
        emailData.getParams().put("notifiedAt", params.get("notifiedAt"));
        emailData.getParams().put("incidentType", params.getOrDefault("incidentType", ""));
        emailData.getParams().put("severity", params.getOrDefault("severity", ""));
        emailData.getParams().put("downtime", params.getOrDefault("downtime", ""));
        return emailData;
    }

    public static EmailDataDto forMonitorStatusUpdated(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setParams(new HashMap<>());
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_MONITOR_STATUS_UPDATED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_MONITOR_STATUS_UPDATED);
        emailData.getParams().put("monitorAddress", params.get("monitorAddress"));
        emailData.getParams().put("statusLabel", params.get("statusLabel"));
        emailData.getParams().put("notifiedAt", params.get("notifiedAt"));
        emailData.getParams().put("incidentType", params.getOrDefault("incidentType", ""));
        emailData.getParams().put("severity", params.getOrDefault("severity", ""));
        return emailData;
    }

    public static EmailDataDto forSmartPlugIncident(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setParams(new HashMap<>());
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_SMART_PLUG_INCIDENT);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_SMART_PLUG_INCIDENT);
        emailData.getParams().put("monitorAddress", params.getOrDefault("monitorAddress", ""));
        emailData.getParams().put("incidentType", params.getOrDefault("incidentType", ""));
        emailData.getParams().put("severity", params.getOrDefault("severity", ""));
        emailData.getParams().put("boxIp", params.getOrDefault("boxIp", ""));
        emailData.getParams().put("notifiedAt", params.getOrDefault("notifiedAt", ""));
        emailData.getParams().put("hypothesis", params.getOrDefault("hypothesis", ""));
        emailData.getParams().put("powerWatts", params.getOrDefault("powerWatts", ""));
        emailData.getParams().put("relayOn", params.getOrDefault("relayOn", ""));
        return emailData;
    }

    public static EmailDataDto forAdminAdRequestQuestionnaireUpdated(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_ADMIN_AD_REQUEST_QUESTIONNAIRE_UPDATED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_AD_REQUEST_QUESTIONNAIRE_UPDATED);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("clientName", params.getOrDefault("clientName", ""));
        emailData.getParams().put("revisionVersion", params.getOrDefault("revisionVersion", ""));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        emailData.getParams().put("adRequestId", params.getOrDefault("adRequestId", ""));
        return emailData;
    }

    public static EmailDataDto forAdminPartnerForeignAdSubmitted(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_ADMIN_PARTNER_FOREIGN_AD_SUBMITTED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_PARTNER_FOREIGN_AD_SUBMITTED);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("partnerName", params.getOrDefault("partnerName", ""));
        emailData.getParams().put("monitorLabel", params.getOrDefault("monitorLabel", ""));
        emailData.getParams().put("adLabel", params.getOrDefault("adLabel", ""));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        return emailData;
    }

    public static EmailDataDto forAdminPartnerPlacementRequest(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_ADMIN_PARTNER_PLACEMENT_REQUEST);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_PARTNER_PLACEMENT_REQUEST);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("partnerName", params.getOrDefault("partnerName", ""));
        emailData.getParams().put("monitorLabel", params.getOrDefault("monitorLabel", params.getOrDefault("monitorsSummary", "")));
        emailData.getParams().put("instructions", params.getOrDefault("instructions", ""));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        return emailData;
    }

    public static EmailDataDto forAdminPartnerFinishedAdSubmitted(Map<String, String> params) {
        EmailDataDto emailData = new EmailDataDto();
        emailData.setSubject(SharedConstants.EMAIL_SUBJECT_ADMIN_PARTNER_FINISHED_AD_SUBMITTED);
        emailData.setTemplate(SharedConstants.TEMPLATE_EMAIL_ADMIN_PARTNER_FINISHED_AD_SUBMITTED);
        emailData.setParams(new HashMap<>());
        emailData.getParams().put("partnerName", params.getOrDefault("partnerName", ""));
        emailData.getParams().put("monitorLabel", params.getOrDefault("monitorLabel", ""));
        emailData.getParams().put("instructions", params.getOrDefault("instructions", ""));
        emailData.getParams().put("link", params.getOrDefault("link", ""));
        return emailData;
    }
}
