package com.telas.helpers;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.telas.dtos.request.*;
import com.telas.entities.*;
import com.telas.enums.AdValidationType;
import com.telas.enums.NotificationReference;
import com.telas.enums.Role;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.*;
import com.telas.services.AddressService;
import com.telas.services.BucketService;
import com.telas.services.MapsService;
import com.telas.services.NotificationService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.*;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.HttpClientUtil;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
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
    private final AddressService addressService;
    private final MapsService mapsService;
    private final HttpClientUtil httpClient;
    private final BucketService bucketService;
    private final NotificationService notificationService;

    @Value("${front.base.url}")
    private String frontBaseUrl;

    @Value("${TOKEN_SECRET}")
    private String API_KEY;

    @Transactional(readOnly = true)
    public void validateClientRequest(ClientRequestDto request, Client client) {
        request.validate();
        verifyUniqueEmail(request, client);
    }

    @Transactional
    public void verifyValidationCode(Client client) {
        if (!client.getVerificationCode().isValidated()) {
            throw new BusinessRuleException(ClientValidationMessages.VALIDATION_CODE_NOT_VALIDATED);
        }
    }

    @Transactional
    public void createAdRequest(ClientAdRequestToAdminDto request, Client client) {
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

        adRequestRepository.save(adRequest);
    }

    @Transactional(readOnly = true)
    public AdRequest getAdRequestById(UUID adRequestId) {
        return adRequestRepository.findById(adRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_REQUEST_NOT_FOUND));
    }


    List<Attachment> getAttachmentsByIds(List<UUID> attachmentsIds) {
        return attachmentRepository.findByIdIn(attachmentsIds).orElseThrow(() -> new ResourceNotFoundException(AttachmentValidationMessages.ATTACHMENTS_NOT_FOUND));
    }

//    void verifyUniqueEmail(ClientRequestDto request, Client client) {
//        String newEmail = request.getContact().getEmail();
//
//        if (client == null && contactRepository.existsByEmail(newEmail)) {
//            throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
//        }
//
//        if (client != null) {
//            if (!client.getContact().getEmail().equals(newEmail) && contactRepository.existsByEmail(newEmail)) {
//                throw new BusinessRuleException(ClientValidationMessages.EMAIL_UNIQUE);
//            }
//        }
//    }

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
        return adRepository.findById(adId).orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_NOT_FOUND));
    }

    @Transactional
    public void updateAddresses(List<AddressRequestDto> requestList, Client client) {
        Set<UUID> receivedAddressIds = requestList.stream()
                .map(AddressRequestDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Address> addressesToRemove = client.getAddresses().stream()
                .filter(address -> address.getMonitors().isEmpty() && !receivedAddressIds.contains(address.getId()))
                .toList();

        requestList.forEach(addressRequest -> {
            Address address = addressRequest.getId() == null
                    ? getOrCreateAddress(addressRequest, client)
                    : updateExistingAddress(addressRequest, client);
            client.getAddresses().add(address);
        });

        if (!addressesToRemove.isEmpty()) {
            addressesToRemove.forEach(client.getAddresses()::remove);
            addressService.deleteMany(addressesToRemove);
        }
    }

    private Address getOrCreateAddress(AddressRequestDto addressRequest, Client client) {
        Address address = addressService.getOrCreateAddress(addressRequest, client);

        if (Role.PARTNER.equals(client.getRole()) && address.getLatitude() == null && address.getLongitude() == null) {
            mapsService.getAddressCoordinates(address);
        }

        return address;
    }

    private Address updateExistingAddress(AddressRequestDto addressRequest, Client client) {
        Address address = addressService.findById(addressRequest.getId());

        if (address.getMonitors().isEmpty() && address.hasChanged(addressRequest)) {
            BeanUtils.copyProperties(addressRequest, address, "latitude", "longitude", "client", "monitors");
            address.setUsernameUpdate(client.getBusinessName());

            if (address.isPartnerAddress()) {
                mapsService.getAddressCoordinates(address);
            }
        }
        return address;
    }

    @Transactional(readOnly = true)
    public void validateAttachmentsCount(Client client, List<AttachmentRequestDto> request) {
        if (!Role.ADMIN.equals(client.getRole())) {
            int newAttachments = (int) request.stream().filter(r -> r.getId() == null).count();
            int totalAttachments = client.getAttachments().size() + newAttachments;

            if (totalAttachments >= SharedConstants.MAX_ATTACHMENT_PER_CLIENT) {
                throw new BusinessRuleException(AttachmentValidationMessages.MAX_ATTACHMENTS_REACHED);
            }
        }
    }

    @Transactional
    @Async
    public void addAdToMonitor(Ad ad, List<UUID> monitorIds, Client client) {
        List<Monitor> monitors = monitorRepository.findAllById(monitorIds);

        if (monitors.size() != monitorIds.size()) {
            log.error("Some monitors not found for ad with id: {}", ad.getId());
            return;
        }

        List<Monitor> monitorsToUpdate = processMonitorsForAd(monitors, ad, client);

        if (!monitorsToUpdate.isEmpty()) {
            sendBoxesMonitorsUpdateAd(ad, monitorsToUpdate);
        }
    }

    @Transactional
    public void addAdToMonitor(Set<Monitor> monitors, Client client) {
        Ad ad = client.getApprovedAd();
        List<Monitor> monitorsToUpdate = processMonitorsForAd(monitors, ad, client);

        if (!monitorsToUpdate.isEmpty()) {
            sendBoxesMonitorsUpdateAd(ad, monitorsToUpdate);
        }
    }

    @Transactional
    public Customer getOrCreateCustomer(Subscription subscription) throws StripeException {
        return getOrCreateCustomer(subscription.getClient());
    }

    @Transactional
    public Customer getOrCreateCustomer(Client client) throws StripeException {
        if (Objects.nonNull(client.getStripeCustomerId())) {
            try {
                return Customer.retrieve(client.getStripeCustomerId());
            } catch (StripeException e) {
                log.warn("Failed to retrieve Customer from Stripe, creating new one.");
            }
        }

        String email = client.getContact().getEmail();
        CustomerSearchResult result = Customer.search(
                CustomerSearchParams.builder()
                        .setQuery("email:'" + email + "'")
                        .build()
        );

        Address clientAddress = client.getAddresses().stream().findFirst().orElse(null);

        Customer customer = result.getData().isEmpty()
                ? createCustomer(client, clientAddress)
                : result.getData().get(0);

        client.setStripeCustomerId(customer.getId());
        clientRepository.save(client);
        return customer;
    }

    private List<Monitor> processMonitorsForAd(Collection<Monitor> monitors, Ad ad, Client client) {
        List<Monitor> monitorsToUpdate = new ArrayList<>();

        monitors.forEach(monitor -> {
            if (isMonitorEligibleForAd(client, monitor)) {
                addMonitorAd(monitor, ad);
                monitorsToUpdate.add(monitor);
            } else {
                log.error("Monitor with id {} is not eligible for ad with id {}", monitor.getId(), ad.getId());
            }
        });

        if (monitorsToUpdate.isEmpty()) {
            log.info("No monitors were eligible for ad with id {}", ad.getId());
            return Collections.emptyList();
        }

        monitorRepository.saveAll(monitorsToUpdate);
        return monitorsToUpdate;
    }

    private boolean isMonitorEligibleForAd(Client client, Monitor monitor) {
        List<Monitor> monitorsWithActiveSubscriptions = monitorRepository.findMonitorsWithActiveSubscriptionsByClientId(client.getId());

        if (!monitorsWithActiveSubscriptions.isEmpty() && !monitorsWithActiveSubscriptions.contains(monitor)) {
            log.error("Client {} does not have an active subscription for monitor {}", client.getId(), monitor.getId());
            return false;
        }

        if (monitor.clientAlreadyHasAd(client)) {
            log.error("Client {} already has an ad in monitor {}", client.getId(), monitor.getId());
            return false;
        }

        if (!monitor.isWithinAdsLimit(SharedConstants.MAX_ADS_PER_CLIENT)) {
            log.error("Monitor with id {} has reached its ad limit for client {}", monitor.getId(), client.getId());
            createAdNotSentToMonitorNotification(monitor, client.getApprovedAd());
            return false;
        }

        return true;
    }

    private void createAdNotSentToMonitorNotification(Monitor monitor, Ad approvedAd) {
        Map<String, String> params = Map.of(
                "adId", approvedAd.getId().toString(),
                "monitorId", monitor.getId().toString(),
                "link", frontBaseUrl + "/admin/screens"
        );

        clientRepository.findAllAdmins().forEach(admin ->
                notificationService.save(NotificationReference.AD_NOT_SENT_TO_MONITOR, admin, params, false)
        );
    }

    private void addMonitorAd(Monitor monitor, Ad ad) {
        monitor.getMonitorAds().add(new MonitorAd(monitor, ad));
    }

    private void sendBoxesMonitorsUpdateAd(Ad ad, List<Monitor> monitors) {
        monitors.stream()
                .filter(Monitor::isAbleToSendBoxRequest)
                .collect(Collectors.groupingBy(Monitor::getBox))
                .forEach((box, boxMonitors) -> {
                    List<UpdateBoxMonitorsAdRequestDto> dtos = boxMonitors.stream()
                            .map(monitor -> new UpdateBoxMonitorsAdRequestDto(
                                    monitor.getId(),
                                    ad.getName(),
                                    bucketService.getLink(AttachmentUtils.format(ad))
                            ))
                            .toList();

                    String url = "http://" + box.getBoxAddress().getIp() + ":8081/ad";
                    Map<String, String> headers = Map.of("X-API-KEY", API_KEY);

                    try {
                        log.info("Sending ad update to box IP: {}, URL: {}", box.getBoxAddress().getIp(), url);
                        httpClient.makePostRequest(url, dtos, Void.class, null, headers);
                    } catch (Exception e) {
                        log.error("Error sending ad update to box IP: {}, URL: {}, message: {}", box.getBoxAddress().getIp(), url, e.getMessage());
                    }
                });
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

    private Customer createCustomer(Client client, Address address) throws StripeException {
        return Customer.create(CustomerCreateParams.builder()
                .setEmail(client.getContact().getEmail())
                .setName(client.getBusinessName())
                .setPhone(client.getContact().getPhone())
                .setAddress(CustomerCreateParams.Address.builder()
                        .setLine1(address != null ? address.getStreet() : null)
                        .setLine2(address != null ? address.getAddress2() : null)
                        .setCity(address != null ? address.getCity() : null)
                        .setState(address != null ? address.getState() : null)
                        .setPostalCode(address != null ? address.getZipCode() : null)
                        .setCountry(address != null ? address.getCountry() : null)
                        .build())
                .build());
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
