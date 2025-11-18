package com.telas.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.MonitorAdRequestDto;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.dtos.request.filters.FilterMonitorRequestDto;
import com.telas.dtos.response.*;
import com.telas.entities.*;
import com.telas.enums.SubscriptionStatus;
import com.telas.helpers.MonitorHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.MonitorRepository;
import com.telas.services.BucketService;
import com.telas.services.MonitorService;
import com.telas.services.SubscriptionService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.PaginationFilterUtil;
import com.telas.shared.utils.ValidateDataUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {
    private final Logger log = LoggerFactory.getLogger(MonitorServiceImpl.class);
    private final AuthenticatedUserService authenticatedUserService;
    private final MonitorRepository repository;
    private final BucketService bucketService;
    private final SubscriptionService subscriptionService;
    private final MonitorHelper helper;

    @Value("${stripe.product.id}")
    private String productId;

    @Override
    @Transactional
    public void save(MonitorRequestDto request, UUID monitorId) throws JsonProcessingException {
        AuthenticatedUser authenticatedUser = authenticatedUserService.validateAdmin();
        request.validate();
        Address address = helper.getAddress(request);

        if (monitorId != null) {
            validateAddressAvailability(address, monitorId);
            List<Ad> ads = !ValidateDataUtils.isNullOrEmpty(request.getAds()) ? helper.getAds(request, monitorId) : Collections.emptyList();
            updateExistingMonitor(request, monitorId, authenticatedUser, address, ads);
        } else {
            validateAddressAvailability(address);
            createNewMonitor(authenticatedUser, address);
        }
    }

    @Override
    @Transactional
    public void removeMonitorAdsFromSubscription(Subscription subscription) {
        List<SubscriptionStatus> validStatuses = List.of(SubscriptionStatus.EXPIRED, SubscriptionStatus.CANCELLED);

        if (!validStatuses.contains(subscription.getStatus())) {
            return;
        }

        Client client = subscription.getClient();
        List<Monitor> updatedMonitors = new ArrayList<>();

        subscription.getMonitors().forEach(monitor -> {
            List<String> adNamesToRemove = monitor.getAds().stream()
                    .filter(ad -> ad.getClient().getId().equals(client.getId()))
                    .map(Ad::getName)
                    .toList();

            if (!adNamesToRemove.isEmpty()) {
                monitor.getMonitorAds().removeIf(monitorAd -> adNamesToRemove.contains(monitorAd.getAd().getName()));
                updatedMonitors.add(monitor);

                if (monitor.isAbleToSendBoxRequest()) {
                    helper.sendBoxesMonitorsRemoveAds(monitor, adNamesToRemove);
                }
            }
        });

        if (!updatedMonitors.isEmpty()) {
            repository.saveAll(updatedMonitors);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MonitorResponseDto findById(UUID monitorId) {
        Monitor entity = repository.findById(monitorId).orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));

        List<MonitorAdResponseDto> adLinks = helper.getMonitorAdsResponse(entity);

        return new MonitorResponseDto(entity, adLinks);
    }

    @Override
    @Transactional
    public Monitor findEntityById(UUID monitorId) {
        return repository.findById(monitorId).orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorMapsResponseDto> findNearestActiveMonitors(String zipCode) {
        UUID clientId = authenticatedUserService.getLoggedUser().client().getId();
        return repository.findAvailableMonitorsByZipCode(zipCode, clientId)
                .stream()
                .map(MonitorMapsResponseDto::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorsBoxMinResponseDto> findAllMonitors() {
        return repository.findAll().stream()
                .map(MonitorsBoxMinResponseDto::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorValidAdResponseDto> findValidAdsForMonitor(UUID monitorId) {
        authenticatedUserService.validateAdmin();
        Monitor monitor = findEntityById(monitorId);
        return helper.getValidAdsForMonitor(monitor);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponseDto<List<MonitorResponseDto>> findAllByFilters(FilterMonitorRequestDto request) {
        authenticatedUserService.validateAdmin();

        Sort order = request.setOrdering();
        Pageable pageable = PaginationFilterUtil.getPageable(request, order);
        Specification<Monitor> filter = PaginationFilterUtil.addSpecificationFilter(
                null,
                request.getGenericFilter(),
                this::filterMonitors
        );

        Page<Monitor> page = repository.findAll(filter, pageable);
        List<MonitorResponseDto> response = page.stream()
                .map(monitor -> new MonitorResponseDto(monitor, helper.getMonitorAdsResponse(monitor)))
                .toList();

        return PaginationResponseDto.fromResult(response, (int) page.getTotalElements(), page.getTotalPages(), request.getPage());
    }

    @Override
    @Transactional
    public void delete(UUID monitorId) {
        authenticatedUserService.validateAdmin();
        Monitor monitor = findEntityById(monitorId);
        ensureNoActiveSubscription(monitor);
        clearMonitorAssociations(monitor);
        repository.delete(monitor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorValidAdResponseDto> findCurrentDisplayedAdsFromBox(UUID monitorId) {
        authenticatedUserService.validateAdmin();
        Monitor monitor = findEntityById(monitorId);
        List<String> adNames = helper.getCurrentDisplayedAdsFromBox(monitor);

        if (adNames.isEmpty()) {
            return List.of();
        }

        return helper.getBoxMonitorAdsResponse(monitor, adNames);
    }

    private void ensureNoActiveSubscription(Monitor monitor) {
        if (repository.existsActiveSubscriptionByMonitorId(monitor.getId())) {
            throw new BusinessRuleException(MonitorValidationMessages.MONITOR_HAS_ACTIVE_SUBSCRIPTION);
        }
    }

    private void clearMonitorAssociations(Monitor monitor) {
        monitor.getMonitorAds().clear();
        monitor.setBox(null);
    }

    private Specification<Monitor> filterMonitors(Specification<Monitor> specification, String genericFilter) {
        return specification.and((root, query, criteriaBuilder) -> {
            String filter = "%" + genericFilter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();


            if ("true".equalsIgnoreCase(genericFilter) || "false".equalsIgnoreCase(genericFilter)) {
                predicates.add(criteriaBuilder.equal(root.get("active"), Boolean.valueOf(genericFilter)));
            }

            Predicate addressPredicate = helper.createAddressPredicate(criteriaBuilder, root, filter);
            predicates.add(addressPredicate);
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        });
    }

    private Monitor createNewMonitor(AuthenticatedUser authenticatedUser, Address address) {
        setCoordinatesIfMissing(address);

        Monitor monitor = new Monitor(address, productId);
        monitor.setUsernameCreate(authenticatedUser.client().getBusinessName());
        repository.save(monitor);
        subscriptionService.savePartnerBonusSubscription(address.getClient(), monitor);
        return monitor;
    }

    private void updateExistingMonitor(MonitorRequestDto request, UUID monitorId, AuthenticatedUser authenticatedUser, Address address, List<Ad> ads) {
        Monitor monitor = findEntityById(monitorId);

        if (isAddressChanged(monitor.getAddress(), address)) {
            ads = handleAddressChange(monitor, address, ads);
        }

        updateMonitorMetadata(authenticatedUser, monitor);
        updateMonitorDetails(request, monitor, ads);
        repository.save(monitor);
    }

    private List<Ad> handleAddressChange(Monitor monitor, Address newAddress, List<Ad> ads) {
        Address oldAddress = monitor.getAddress();
        Client oldPartner = getClientFromAddress(oldAddress);
        Client newPartner = newAddress.getClient();

        monitor.setAddress(newAddress);
        handlePartnerSubscriptionChanges(oldPartner, newPartner, monitor);
        setCoordinatesIfMissing(newAddress);
        return addNewPartnerAdIfExists(ads, newPartner);
    }

    private List<Ad> addNewPartnerAdIfExists(List<Ad> ads, Client newPartner) {
        Ad approvedAd = newPartner == null ? null : newPartner.getApprovedAd();

        if (approvedAd == null) {
            return ads;
        }

        List<Ad> mutable = ValidateDataUtils.isNullOrEmpty(ads) ? new ArrayList<>() : ads;
        List<UUID> adIds = mutable.stream().map(Ad::getId).toList();

        if (!adIds.contains(approvedAd.getId())) {
            mutable.add(approvedAd);
        }

        return mutable;
    }


    private void validateAddressAvailability(Address address) {
        if (repository.existsByAddressId(address.getId())) {
            throw new BusinessRuleException(MonitorValidationMessages.ADDRESS_ALREADY_IN_USE);
        }
    }

    private void validateAddressAvailability(Address address, UUID monitorId) {
        if (repository.existsByAddressIdAndIdNot(address.getId(), monitorId)) {
            throw new BusinessRuleException(MonitorValidationMessages.ADDRESS_ALREADY_IN_USE);
        }
    }

    private void setCoordinatesIfMissing(Address address) {
        if (!address.hasLocation()) {
            helper.setAddressCoordinates(address);
        }
    }

    private Client getClientFromAddress(Address address) {
        return address != null ? address.getClient() : null;
    }

    private boolean isAddressChanged(Address oldAddress, Address newAddress) {
        return !Objects.equals(oldAddress != null ? oldAddress.getId() : null, newAddress.getId());
    }

    private void handlePartnerSubscriptionChanges(Client oldPartner, Client newPartner, Monitor monitor) {
        if (oldPartner != null && oldPartner.isPartner()) {
            cancelPartnerBonusSubscription(oldPartner);
        }
        if (newPartner != null && newPartner.isPartner()) {
            subscriptionService.savePartnerBonusSubscription(newPartner, monitor);
        }
    }

    private void cancelPartnerBonusSubscription(Client partner) {
        try {
            subscriptionService.cancelBonusSubscription(partner);
        } catch (ResourceNotFoundException e) {
            log.info("Old partner {} had no bonus subscription to cancel.", partner.getId());
        }
    }

    private void updateMonitorMetadata(AuthenticatedUser authenticatedUser, Monitor monitor) {
        String usernameUpdate = authenticatedUser.client().getBusinessName();
        CustomRevisionListener.setUsername(usernameUpdate);
        monitor.setUsernameUpdate(usernameUpdate);
    }

    private void updateMonitorDetails(MonitorRequestDto request, Monitor monitor, List<Ad> ads) {
        monitor.setProductId(productId);
        monitor.setActive(request.getActive() != null ? request.getActive() : monitor.isActive());

        if (!ValidateDataUtils.isNullOrEmpty(ads)) {
            updateMonitorAds(request, monitor, ads);
        }
    }

    private void updateMonitorAds(MonitorRequestDto request, Monitor monitor, List<Ad> ads) {
        Set<UUID> newAdIds = buildNewAdIds(ads);

        removeStaleMonitorAds(monitor, newAdIds);
        validateAdsLimit(monitor, ads);

        Map<UUID, MonitorAdRequestDto> adRequestMap = mapAdsById(request);
        Map<UUID, MonitorAd> existingAfterRemoval = buildExistingAfterRemoval(monitor);
        Map<UUID, SubscriptionMonitor> subscriptionByClientId = buildSubscriptionByClientId(monitor);

        List<MonitorAd> newMonitorAds = createNewMonitorAds(ads, existingAfterRemoval, adRequestMap, monitor);
        updateOrderIndexes(ads, existingAfterRemoval, adRequestMap);

        Map<UUID, MonitorAd> newMonitorAdsByAdId = newMonitorAds.stream()
                .collect(Collectors.toMap(ma -> ma.getAd().getId(), ma -> ma));

        // agora passamos o monitor para que possamos identificar o partner atual
        List<UpdateBoxMonitorsAdRequestDto> requestList = buildRequestList(ads, existingAfterRemoval, newMonitorAdsByAdId, subscriptionByClientId, monitor);

        addNewMonitorAdsToMonitor(monitor, newMonitorAds);

        if (monitor.isAbleToSendBoxRequest()) {
            helper.sendBoxesMonitorsUpdateAds(requestList);
        }
    }

    private Set<UUID> buildNewAdIds(List<Ad> ads) {
        return ads.stream().map(Ad::getId).collect(Collectors.toSet());
    }

    private void removeStaleMonitorAds(Monitor monitor, Set<UUID> newAdIds) {
        monitor.getMonitorAds().removeIf(ma -> !newAdIds.contains(ma.getAd().getId()));
    }

    private Map<UUID, MonitorAd> buildExistingAfterRemoval(Monitor monitor) {
        return monitor.getMonitorAds().stream()
                .collect(Collectors.toMap(ma -> ma.getAd().getId(), ma -> ma));
    }

    private Map<UUID, SubscriptionMonitor> buildSubscriptionByClientId(Monitor monitor) {
        List<SubscriptionMonitor> subscriptionMonitors = helper.getSubscriptionsMonitorsFromMonitor(monitor.getId());
        return subscriptionMonitors.stream()
                .filter(sm -> sm.getSubscription() != null && sm.getSubscription().getClient() != null && sm.getSubscription().getClient().getId() != null)
                .collect(Collectors.toMap(sm -> sm.getSubscription().getClient().getId(), sm -> sm, (a, b) -> a));
    }

    private List<MonitorAd> createNewMonitorAds(List<Ad> ads, Map<UUID, MonitorAd> existingAfterRemoval,
                                                Map<UUID, MonitorAdRequestDto> adRequestMap, Monitor monitor) {
        return ads.stream()
                .filter(ad -> !existingAfterRemoval.containsKey(ad.getId()))
                .map(ad -> {
                    MonitorAdRequestDto reqDto = adRequestMap.get(ad.getId());
                    // Se não veio MonitorAdRequestDto (ex.: ad do partner inserido automaticamente), utiliza outro construtor
                    return reqDto != null ? new MonitorAd(reqDto, monitor, ad) : new MonitorAd(monitor, ad);
                })
                .toList();
    }

    private void updateOrderIndexes(List<Ad> ads, Map<UUID, MonitorAd> existingAfterRemoval, Map<UUID, MonitorAdRequestDto> adRequestMap) {
        ads.stream()
                .filter(ad -> existingAfterRemoval.containsKey(ad.getId()))
                .forEach(ad -> {
                    MonitorAd ma = existingAfterRemoval.get(ad.getId());
                    MonitorAdRequestDto dto = adRequestMap.get(ad.getId());
                    if (ma != null && dto != null) {
                        ma.setOrderIndex(dto.getOrderIndex());
                    }
                });
    }

    private List<UpdateBoxMonitorsAdRequestDto> buildRequestList(List<Ad> ads,
                                                                 Map<UUID, MonitorAd> existingAfterRemoval,
                                                                 Map<UUID, MonitorAd> newMonitorAdsByAdId,
                                                                 Map<UUID, SubscriptionMonitor> subscriptionByClientId,
                                                                 Monitor monitor) {
        return ads.stream()
                .map(ad -> {
                    MonitorAd monitorAd = existingAfterRemoval.getOrDefault(ad.getId(), newMonitorAdsByAdId.get(ad.getId()));
                    UUID clientId = ad.getClient() != null ? ad.getClient().getId() : null;
                    SubscriptionMonitor matched = clientId != null ? subscriptionByClientId.get(clientId) : null;
                    String link = bucketService.getLink(AttachmentUtils.format(ad));
                    UpdateBoxMonitorsAdRequestDto dto = matched != null
                            ? new UpdateBoxMonitorsAdRequestDto(ad, monitorAd, matched, link)
                            : new UpdateBoxMonitorsAdRequestDto(ad, monitorAd, link);

                    // Se o ad pertence ao partner atual do monitor, força blockQuantity para o valor reservado (7)
                    if (monitor != null
                            && monitor.getAddress() != null
                            && monitor.getAddress().getClient() != null
                            && ad.getClient() != null
                            && ad.getClient().isPartner()
                            && Objects.equals(ad.getClient().getId(), monitor.getAddress().getClient().getId())) {
                        dto.setBlockQuantity(SharedConstants.PARTNER_RESERVED_SLOTS);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void validateAdsLimit(Monitor monitor, List<Ad> newAds) {
        UUID partnerId = monitor.getAddress() != null && monitor.getAddress().getClient() != null
                ? monitor.getAddress().getClient().getId()
                : null;

        long partnerAdsCountFinal = newAds.stream()
                .filter(ad -> ad.getClient() != null && ad.getClient().isPartner()
                        && Objects.equals(ad.getClient().getId(), partnerId))
                .count();

        int otherAdsFinal = newAds.size() - (int) partnerAdsCountFinal;
        int availableSlotsForOthers = monitor.getMaxBlocks() - SharedConstants.PARTNER_RESERVED_SLOTS;

        if (otherAdsFinal > availableSlotsForOthers) {
            throw new BusinessRuleException(MonitorValidationMessages.ADS_LIMIT_EXCEEDED + monitor.getMaxBlocks());
        }
    }

    private void addNewMonitorAdsToMonitor(Monitor monitor, List<MonitorAd> newMonitorAds) {
        if (!newMonitorAds.isEmpty()) {
            monitor.getMonitorAds().addAll(newMonitorAds);
        }
    }

    private Map<UUID, MonitorAdRequestDto> mapAdsById(MonitorRequestDto request) {
        return request.getAds().stream()
                .collect(Collectors.toMap(MonitorAdRequestDto::getId, dto -> dto));
    }
}
