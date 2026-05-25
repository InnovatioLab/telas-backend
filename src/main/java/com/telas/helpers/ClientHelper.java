package com.telas.helpers;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.telas.dtos.request.*;
import com.telas.entities.*;
import com.telas.enums.AdValidationType;
import com.telas.enums.NotificationReference;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.*;
import com.telas.services.BucketService;
import com.telas.services.NotificationService;
import com.telas.services.PartnerSlotAccessService;
import com.telas.services.box.BoxPlaylistClient;
import com.telas.services.client.ClientAddressService;
import com.telas.services.payment.StripeSubscriptionLifecycle;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.*;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClientHelper {
    private final Logger log = LoggerFactory.getLogger(ClientHelper.class);
    private final ClientRepository clientRepository;
    private final MonitorRepository monitorRepository;
    private final ContactRepository contactRepository;
    private final AttachmentRepository attachmentRepository;
    private final AdRepository adRepository;
    private final AdRequestRepository adRequestRepository;
    private final SubscriptionMonitorRepository subscriptionMonitorRepository;
    private final ClientAddressService clientAddressService;
    private final BucketService bucketService;
    private final NotificationService notificationService;
    private final BoxAdPushNotificationHelper boxAdPushNotificationHelper;
    private final PartnerSlotAccessService partnerSlotAccessService;
    private final BoxPlaylistClient boxPlaylistClient;
    private final StripeSubscriptionLifecycle stripeSubscriptionLifecycle;

    @Value("${front.base.url}")
    private String frontBaseUrl;

    @Transactional(readOnly = true)
    public void validateClientRequest(ClientRequestDto request, Client client) {
        request.validate();
        validateReservedBusinessName(request.getBusinessName());
        verifyUniqueEmail(request, client);
    }

    private void validateReservedBusinessName(String businessName) {
        if (businessName == null) {
            return;
        }
        String normalized = businessName.trim();
        if (normalized.equalsIgnoreCase("admin")) {
            throw new BusinessRuleException(ClientValidationMessages.BUSINESS_NAME_RESERVED);
        }
    }

    @Transactional
    public void verifyAddressesUnique(List<AddressRequestDto> addresses, Client client) {
        clientAddressService.verifyAddressesUnique(addresses, client);
    }

    @Transactional
    public void verifyValidationCode(Client client) {
        if (!client.getVerificationCode().isValidated()) {
            throw new BusinessRuleException(ClientValidationMessages.VALIDATION_CODE_NOT_VALIDATED);
        }
    }

    @Transactional
    public AdRequest createAdRequest(ClientAdRequestToAdminDto request, Client client) {
        List<Attachment> attachments = !request.getAttachmentIds().isEmpty() ? getAttachmentsByIds(request.getAttachmentIds()) : null;

        if (Objects.nonNull(attachments) && attachments.stream().anyMatch(attachment -> attachment.getClient() != null && !attachment.getClient().getId().equals(client.getId()))) {
            throw new BusinessRuleException(AttachmentValidationMessages.ATTACHMENTS_NOT_BELONG_TO_CLIENT);
        }

        AdRequest adRequest = new AdRequest(request, client, attachments);

        Ad rejectedAd = client.getAds().stream()
                .filter(ad -> AdValidationType.REJECTED.equals(ad.getValidation()))
                .findFirst()
                .orElse(null);

        if (Objects.nonNull(rejectedAd)) {
            adRequest.setAd(rejectedAd);
            rejectedAd.setAdRequest(adRequest);
            adRepository.save(rejectedAd);
        }

        return adRequestRepository.save(adRequest);
    }

    @Transactional
    public AdRequest createPartnerAdRequest(PartnerAdRequestToAdminDto request, Client partner, Monitor targetMonitor) {
        if (adRequestRepository.existsByClientIdAndRequestOriginAndTargetMonitorIdAndIsActiveTrueAndSubmissionMode(
                partner.getId(),
                com.telas.enums.AdRequestOrigin.PARTNER,
                targetMonitor.getId(),
                com.telas.enums.PartnerSubmissionMode.ADMIN_MATERIALS)) {
            throw new BusinessRuleException(ClientValidationMessages.AD_REQUEST_EXISTS);
        }

        List<Attachment> attachments = getAttachmentsByIds(request.getAttachmentIds());
        if (attachments.stream().anyMatch(attachment ->
                attachment.getClient() != null && !attachment.getClient().getId().equals(partner.getId()))) {
            throw new BusinessRuleException(AttachmentValidationMessages.ATTACHMENTS_NOT_BELONG_TO_CLIENT);
        }

        AdRequest adRequest = new AdRequest(request, partner, targetMonitor, attachments);
        return adRequestRepository.save(adRequest);
    }

    @Transactional
    public AdRequest createPartnerFinishedCreativeRequest(
            PartnerAdRequestToAdminDto request,
            Client partner,
            Monitor targetMonitor,
            AttachmentRequestDto attachmentRequest) {
        if (adRequestRepository.existsByClientIdAndRequestOriginAndTargetMonitorIdAndIsActiveTrueAndSubmissionMode(
                partner.getId(),
                com.telas.enums.AdRequestOrigin.PARTNER,
                targetMonitor.getId(),
                com.telas.enums.PartnerSubmissionMode.PARTNER_FINISHED_CREATIVE)) {
            throw new BusinessRuleException(ClientValidationMessages.AD_REQUEST_EXISTS);
        }

        AdRequest adRequest = new AdRequest(request, partner, targetMonitor, List.of());
        adRequest.setSubmissionMode(com.telas.enums.PartnerSubmissionMode.PARTNER_FINISHED_CREATIVE);
        adRequest.setAttachmentIds("");
        adRequestRepository.save(adRequest);

        Ad ad = new Ad(attachmentRequest, partner, adRequest);
        ad.setValidation(AdValidationType.PENDING);
        ad.setUsernameCreate(partner.getBusinessName());
        if (request.getOptionalLabel() != null && !request.getOptionalLabel().isBlank()) {
            ad.setName(request.getOptionalLabel().trim());
        }
        adRepository.save(ad);
        partner.getAds().add(ad);
        clientRepository.save(partner);

        bucketService.upload(
                attachmentRequest.getBytes(),
                AttachmentUtils.format(ad),
                attachmentRequest.getType(),
                new java.io.ByteArrayInputStream(attachmentRequest.getBytes())
        );

        return adRequest;
    }

    @Transactional(readOnly = true)
    public AdRequest getAdRequestById(UUID adRequestId) {
        return adRequestRepository.findById(adRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_REQUEST_NOT_FOUND));
    }

    List<Attachment> getAttachmentsByIds(List<UUID> attachmentsIds) {
        return attachmentRepository.findByIdIn(attachmentsIds).orElseThrow(() -> new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENTS_NOT_FOUND));
    }

    void verifyUniqueEmail(ClientRequestDto request, Client client) {
        String newEmail = request.getContact().getEmail();

        if (contactRepository.existsByEmail(newEmail)) {
            boolean isNewClient = client == null;
            boolean isEmailChanged = client != null && !client.getContact().getEmail().equals(newEmail);

            if (isNewClient || isEmailChanged) {
                throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
            }
        }
    }

    @Transactional(readOnly = true)
    public Ad getAdById(UUID adId) {
        return adRepository.findByIdWithClientAndAdRequest(adId)
                .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_NOT_FOUND));
    }

    @Transactional
    public void updateAddresses(List<AddressRequestDto> requestList, Client client) {
        clientAddressService.updateAddresses(requestList, client);
    }

    @Transactional(readOnly = true)
    public void validateAttachmentsCount(Client client, List<AttachmentRequestDto> request) {
        if (!client.isPrivilegedPanelUser()) {
            int newAttachments = (int) request.stream().filter(r -> r.getId() == null).count();
            int totalAttachments = client.getAttachments().size() + newAttachments;

            if (totalAttachments > SharedConstants.MAX_ATTACHMENT_PER_CLIENT) {
                throw new BusinessRuleException(AttachmentValidationMessages.MAX_ATTACHMENTS_REACHED);
            }
        }
    }

    @Transactional
    public void addAdToMonitor(List<Ad> ads, Subscription subscription) {
        if (ValidateDataUtils.isNullOrEmpty(ads)) {
            return;
        }
        List<SubscriptionMonitor> subscriptionMonitors = subscription.getSubscriptionMonitors().stream().toList();
        Client client = subscription.getClient();

        Map<Monitor, List<AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto>>> grouped = groupAdsByMonitor(ads, subscriptionMonitors, client);

        if (grouped.isEmpty()) {
            return;
        }

        persistAndNotify(grouped);
    }

    @Transactional
    public void addAdToMonitor(List<Ad> ads, Client client) {
        if (ValidateDataUtils.isNullOrEmpty(ads)) {
            return;
        }
        List<SubscriptionMonitor> subscriptionMonitors = subscriptionMonitorRepository.findByClientId(client.getId());

        if (subscriptionMonitors.isEmpty()) {
            return;
        }

        Map<Monitor, List<AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto>>> grouped = groupAdsByMonitor(ads, subscriptionMonitors, client);

        if (grouped.isEmpty()) {
            return;
        }

        persistAndNotify(grouped);
    }

    private Map<Monitor, List<AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto>>> groupAdsByMonitor(
            List<Ad> ads,
            List<SubscriptionMonitor> subscriptionMonitors,
            Client client) {

        return ads.stream()
                .filter(Objects::nonNull)
                .flatMap(ad -> subscriptionMonitors.stream().map(sm -> new AbstractMap.SimpleEntry<>(ad, sm)))
                .filter(entry -> isMonitorEligibleForAd(client, entry.getValue().getMonitor()))
                .map(entry -> {
                    Ad ad = entry.getKey();
                    SubscriptionMonitor sm = entry.getValue();
                    Monitor monitor = sm.getMonitor();

                    MonitorAd monitorAd = new MonitorAd(monitor, ad);
                    monitorAd.setBlockQuantity(sm.getSlotsQuantity());

                    UpdateBoxMonitorsAdRequestDto dto = monitor.isAbleToSendBoxRequest()
                            ? new UpdateBoxMonitorsAdRequestDto(ad, monitorAd, sm, bucketService.getLink(AttachmentUtils.format(ad)))
                            : null;

                    return new AbstractMap.SimpleEntry<>(monitor, new AbstractMap.SimpleEntry<>(monitorAd, dto));
                })
                .collect(Collectors.groupingBy(
                        AbstractMap.SimpleEntry::getKey,
                        Collectors.mapping(AbstractMap.SimpleEntry::getValue, Collectors.toList())
                ));
    }

    private void persistAndNotify(Map<Monitor, List<AbstractMap.SimpleEntry<MonitorAd, UpdateBoxMonitorsAdRequestDto>>> grouped) {
        List<Monitor> monitorsToUpdate = grouped.entrySet().stream()
                .peek(entry -> entry.getValue().forEach(pair -> entry.getKey().getMonitorAds().add(pair.getKey())))
                .map(Map.Entry::getKey)
                .toList();

        List<UpdateBoxMonitorsAdRequestDto> requestList = grouped.values().stream()
                .flatMap(Collection::stream)
                .map(AbstractMap.SimpleEntry::getValue)
                .filter(Objects::nonNull)
                .toList();

        if (!monitorsToUpdate.isEmpty()) {
            monitorRepository.saveAll(monitorsToUpdate);
        }

        if (!requestList.isEmpty()) {
            Set<String> successfulBaseUrls = boxPlaylistClient.pushPlaylistUpdates(requestList);
            if (!successfulBaseUrls.isEmpty()) {
                boxAdPushNotificationHelper.notifyAfterSuccessfulPush(grouped, successfulBaseUrls);
            }
        }
    }

    @Transactional
    public Customer getOrCreateCustomer(Subscription subscription) throws StripeException {
        return stripeSubscriptionLifecycle.getOrCreateCustomer(subscription);
    }

    @Transactional
    public Customer getOrCreateCustomer(Client client) throws StripeException {
        return stripeSubscriptionLifecycle.getOrCreateCustomer(client);
    }

    private boolean isMonitorEligibleForAd(Client client, Monitor monitor) {
        if (monitor.clientAlreadyHasAd(client)
                && !partnerSlotAccessService.usesPartnerQuotaOnMonitor(client, monitor)) {
            log.error("Client {} already has an ad in monitor {}", client.getId(), monitor.getId());
            return false;
        }

        if (partnerSlotAccessService.usesPartnerQuotaOnMonitor(client, monitor)
                && partnerSlotAccessService.usedBlocksByClientOnMonitor(client, monitor)
                        >= SharedConstants.PARTNER_RESERVED_SLOTS) {
            log.error(
                    "Partner {} reached block limit on monitor {}",
                    client.getId(),
                    monitor.getId());
            return false;
        }

        if (!monitor.isWithinAdsLimit(SharedConstants.MAX_ADS_PER_CLIENT)) {
            log.error("Monitor with id {} has reached its ad limit for client {}", monitor.getId(), client.getId());
            createAdNotSentToMonitorNotification(monitor, client.getApprovedAds());
            return false;
        }

        return true;
    }

    private void createAdNotSentToMonitorNotification(Monitor monitor, List<Ad> approvedAds) {
        String adIdsList = approvedAds.stream()
                .map(ad -> ad.getId().toString())
                .collect(Collectors.joining(", "));

        Map<String, String> params = Map.of(
                "adIds", adIdsList,
                "monitorId", monitor.getId().toString(),
                "link", frontBaseUrl + "/admin/screens"
        );

        clientRepository.findAllAdmins().forEach(admin ->
                notificationService.save(NotificationReference.AD_NOT_SENT_TO_MONITOR, admin, params, false)
        );
    }

    @Transactional
    public void addMonitorToWishlist(UUID monitorId, Client client) {
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));

        if (client.getWishlist().getMonitors().stream().anyMatch(m -> m.getId().equals(monitorId))) {
            log.info("Monitor with id {} already exists in wishlist for client {}", monitorId, client.getId());
            return;
        }

        if (monitorRepository.findMonitorsWithActiveSubscriptionsByClientId(client.getId()).stream().anyMatch(m -> m.getId().equals(monitorId))) {
            throw new BusinessRuleException(ClientValidationMessages.MONITOR_IN_ACTIVE_SUBSCRIPTION);
        }

        client.getWishlist().getMonitors().add(monitor);
        clientRepository.save(client);
    }

    @Transactional(readOnly = true)
    public List<Monitor> findClientMonitorsWithActiveSubscriptions(UUID id) {
        return monitorRepository.findMonitorsWithActiveSubscriptionsByClientId(id);
    }

    @Transactional
    public void validateEmail(String email) {
        if (ValidateDataUtils.isNullOrEmptyString(email)) {
            throw new BusinessRuleException(ContactValidationMessages.EMAIL_REQUIRED);
        }

        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessRuleException(ContactValidationMessages.EMAIL_INVALID);
        }
    }
}
