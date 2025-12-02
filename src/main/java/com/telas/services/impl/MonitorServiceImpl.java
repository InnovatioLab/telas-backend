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
import com.telas.shared.utils.MonitorBlocksUtils;
import com.telas.shared.utils.PaginationFilterUtil;
import com.telas.shared.utils.ValidateDataUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
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
import java.util.concurrent.atomic.AtomicInteger;
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
                .map(monitor -> {
                    List<SubscriptionMonitor> subscriptionMonitors = helper.getSubscriptionsMonitorsFromMonitor(monitor.getId());

                    int totalSubscriptionBlocks = MonitorBlocksUtils.sumSubscriptionBlocks(subscriptionMonitors);

                    Map<UUID, SubscriptionMonitor> subsByClientId = subscriptionMonitors.stream()
                            .filter(sm -> sm.getSubscription() != null && sm.getSubscription().getClient() != null)
                            .collect(Collectors.toMap(sm -> sm.getSubscription().getClient().getId(), sm -> sm, (a, b) -> a));

                    long unmatchedAdsCount = monitor.getMonitorAds().stream()
                            .filter(ma -> {
                                UUID clientIdAd = ma.getAd() != null && ma.getAd().getClient() != null
                                        ? ma.getAd().getClient().getId()
                                        : null;
                                return clientIdAd == null || !subsByClientId.containsKey(clientIdAd);
                            })
                            .count();

                    int adsDailyMinutes = MonitorBlocksUtils
                            .calculateAdsDailyDisplayTimeInMinutes(monitor.getMaxBlocks(), totalSubscriptionBlocks, unmatchedAdsCount);

                    return new MonitorMapsResponseDto(monitor, adsDailyMinutes);
                })
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

    private void createNewMonitor(AuthenticatedUser authenticatedUser, Address address) {
        setCoordinatesIfMissing(address);

        Monitor monitor = new Monitor(address, productId);
        monitor.setUsernameCreate(authenticatedUser.client().getBusinessName());
        repository.save(monitor);
        subscriptionService.savePartnerBonusSubscription(address.getClient(), monitor);
    }

    private void updateExistingMonitor(MonitorRequestDto request, UUID monitorId, AuthenticatedUser authenticatedUser, Address address, List<Ad> ads) {
        Monitor monitor = findEntityById(monitorId);

        if (isAddressChanged(monitor.getAddress(), address)) {
            ads = handleAddressChange(monitor, address, ads, request);
        }

        updateMonitorMetadata(authenticatedUser, monitor);
        updateMonitorDetails(request, monitor, ads);
        repository.save(monitor);
    }

    private List<Ad> handleAddressChange(Monitor monitor, Address newAddress, List<Ad> ads, MonitorRequestDto request) {
        Address oldAddress = monitor.getAddress();
        Client oldPartner = getClientFromAddress(oldAddress);
        Client newPartner = newAddress.getClient();

        monitor.setAddress(newAddress);
        handlePartnerSubscriptionChanges(oldPartner, newPartner, monitor);
        setCoordinatesIfMissing(newAddress);

        boolean hasSpaceForPartner = hasSpaceForPartnerAds(request, ads, newPartner);

        List<Ad> updatedAds = hasSpaceForPartner ? addNewPartnerAds(ads, newPartner) : ads;

        if (hasSpaceForPartner && !ValidateDataUtils.isNullOrEmpty(newPartner.getApprovedAds())) {
            addPartnerAdsToRequest(newPartner, request, updatedAds);
        }

        return updatedAds;
    }

    private List<Ad> addNewPartnerAds(List<Ad> ads, Client newPartner) {
        if (newPartner == null || ValidateDataUtils.isNullOrEmpty(newPartner.getApprovedAds())) {
            return ads;
        }

        List<Ad> mutable = ValidateDataUtils.isNullOrEmpty(ads) ? new ArrayList<>() : new ArrayList<>(ads);
        Set<UUID> adIds = mutable.stream()
                .map(Ad::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        newPartner.getApprovedAds().stream()
                .filter(ad -> ad != null && ad.getId() != null && !adIds.contains(ad.getId()))
                .forEach(mutable::add);

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

        // Validar e ajustar blockQuantity antes de validar limite
//        validateAndAdjustBlockQuantities(request, monitor, ads);

        Map<UUID, MonitorAdRequestDto> adRequestMap = mapAdsById(request);
        Map<UUID, MonitorAd> existingAfterRemoval = buildExistingAfterRemoval(monitor);
        Map<UUID, SubscriptionMonitor> subscriptionByClientId = buildSubscriptionByClientId(monitor);

        List<MonitorAd> newMonitorAds = createNewMonitorAds(ads, existingAfterRemoval, adRequestMap, monitor);
        updateOrderIndexes(ads, existingAfterRemoval, adRequestMap);

        Map<UUID, MonitorAd> newMonitorAdsByAdId = newMonitorAds.stream()
                .collect(Collectors.toMap(ma -> ma.getAd().getId(), ma -> ma));

        // Garantir orderIndex sequencial
//        ensureSequentialOrderIndex(ads, existingAfterRemoval, newMonitorAdsByAdId, adRequestMap);

//        List<UpdateBoxMonitorsAdRequestDto> requestList = buildRequestList(ads, existingAfterRemoval, newMonitorAdsByAdId, subscriptionByClientId, monitor, adRequestMap);
        List<UpdateBoxMonitorsAdRequestDto> requestList = buildRequestList(ads, existingAfterRemoval, newMonitorAdsByAdId, subscriptionByClientId, adRequestMap);

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

//    private List<UpdateBoxMonitorsAdRequestDto> buildRequestList(List<Ad> ads,
//                                                                 Map<UUID, MonitorAd> existingAfterRemoval,
//                                                                 Map<UUID, MonitorAd> newMonitorAdsByAdId,
//                                                                 Map<UUID, SubscriptionMonitor> subscriptionByClientId,
//                                                                 Monitor monitor,
//                                                                 Map<UUID, MonitorAdRequestDto> adRequestMap) {
//
//        UUID partnerId = monitor.getAddress().getClient().getId();
//        Map<Boolean, List<Ad>> partitionedAds = ads.stream()
//                .collect(Collectors.partitioningBy(ad ->
//                        ad.getClient().isPartner()
//                                && Objects.equals(ad.getClient().getId(), partnerId)));
//
//        List<Ad> partnerAds = partitionedAds.get(true);
//        List<Ad> otherAds = partitionedAds.get(false);
//
//        List<UpdateBoxMonitorsAdRequestDto> requestList = new ArrayList<>();
//
//        // Para ads do partner, usar o blockQuantity que já está definido no MonitorAdRequestDto
//        partnerAds.forEach(ad -> {
//            MonitorAd monitorAd = existingAfterRemoval.getOrDefault(ad.getId(), newMonitorAdsByAdId.get(ad.getId()));
//            String link = bucketService.getLink(AttachmentUtils.format(ad));
//            UpdateBoxMonitorsAdRequestDto dto = new UpdateBoxMonitorsAdRequestDto(ad, monitorAd, link);
//
//            // Usar o blockQuantity do MonitorAdRequestDto (já foi distribuído no addPartnerAdsToRequest)
//            MonitorAdRequestDto reqDto = adRequestMap.get(ad.getId());
//            if (reqDto != null && reqDto.getBlockQuantity() != null) {
//                dto.setBlockQuantity(reqDto.getBlockQuantity());
//            }
//
//            requestList.add(dto);
//        });
//
//        otherAds.forEach(ad -> {
//            MonitorAd monitorAd = existingAfterRemoval.getOrDefault(ad.getId(), newMonitorAdsByAdId.get(ad.getId()));
//            UUID clientId = ad.getClient().getId();
//            SubscriptionMonitor matched = subscriptionByClientId.get(clientId);
//            String link = bucketService.getLink(AttachmentUtils.format(ad));
//
//            UpdateBoxMonitorsAdRequestDto dto = new UpdateBoxMonitorsAdRequestDto(ad, monitorAd, link);
//            MonitorAdRequestDto reqDto = adRequestMap.get(ad.getId());
//
//            // Definir blockQuantity conforme a regra:
//            // - Se houver SubscriptionMonitor e slotsQuantity != 7: usar slotsQuantity
//            // - Se houver SubscriptionMonitor mas slotsQuantity == 7: usar blockQuantity do MonitorAdRequestDto
//            // - Se não houver SubscriptionMonitor: usar blockQuantity do MonitorAdRequestDto
//            if (matched != null && matched.getSlotsQuantity() != null
//                    && matched.getSlotsQuantity() != SharedConstants.PARTNER_RESERVED_SLOTS) {
//                // Usar slotsQuantity do SubscriptionMonitor (exceto quando for 7)
//                dto.setBlockQuantity(matched.getSlotsQuantity());
//            } else if (reqDto != null && reqDto.getBlockQuantity() != null) {
//                // Usar blockQuantity do MonitorAdRequestDto
//                dto.setBlockQuantity(reqDto.getBlockQuantity());
//            }
//
//            requestList.add(dto);
//        });
//
//        int totalBlockQuantity = requestList.stream()
//                .mapToInt(dto -> dto.getBlockQuantity() != null ? dto.getBlockQuantity() : 0)
//                .sum();
//
//        if (totalBlockQuantity > SharedConstants.MAX_MONITOR_ADS) {
//            throw new BusinessRuleException(MonitorValidationMessages.MONITOR_BLOCKS_BEYOND_LIMIT);
//        }
//
//        return requestList;
//    }

    private List<UpdateBoxMonitorsAdRequestDto> buildRequestList(List<Ad> ads,
                                                                 Map<UUID, MonitorAd> existingAfterRemoval,
                                                                 Map<UUID, MonitorAd> newMonitorAdsByAdId,
                                                                 Map<UUID, SubscriptionMonitor> subscriptionByClientId,
                                                                 Map<UUID, MonitorAdRequestDto> adRequestMap) {
        List<UpdateBoxMonitorsAdRequestDto> requestList = new ArrayList<>();

        ads.forEach(ad -> {
            MonitorAd monitorAd = existingAfterRemoval.getOrDefault(ad.getId(), newMonitorAdsByAdId.get(ad.getId()));
            String link = bucketService.getLink(AttachmentUtils.format(ad));
            UpdateBoxMonitorsAdRequestDto dto = new UpdateBoxMonitorsAdRequestDto(ad, monitorAd, link);

            MonitorAdRequestDto reqDto = adRequestMap.get(ad.getId());
            Integer blockQuantity = null;

            SubscriptionMonitor matched = subscriptionByClientId.get(ad.getClient().getId());
            if (matched != null && matched.getSlotsQuantity() != SharedConstants.PARTNER_RESERVED_SLOTS) {
                blockQuantity = matched.getSlotsQuantity();
            } else if (reqDto != null && reqDto.getBlockQuantity() != null) {
                blockQuantity = reqDto.getBlockQuantity();
            }

            dto.setBlockQuantity(blockQuantity);
            requestList.add(dto);
        });

        int totalBlockQuantity = requestList.stream()
                .mapToInt(dto -> dto.getBlockQuantity() != null ? dto.getBlockQuantity() : 0)
                .sum();

        if (totalBlockQuantity > SharedConstants.MAX_MONITOR_ADS) {
            throw new BusinessRuleException(MonitorValidationMessages.MONITOR_BLOCKS_BEYOND_LIMIT);
        }

        return requestList;
    }


    private void validateAdsLimit(Monitor monitor, List<Ad> newAds) {
        UUID partnerId = monitor.getAddress() != null && monitor.getAddress().getClient() != null
                ? monitor.getAddress().getClient().getId()
                : null;

        long partnerAdsCountFinal = newAds.stream()
                .filter(ad -> ad.getClient().isPartner()
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

    private boolean hasSpaceForPartnerAds(MonitorRequestDto request, List<Ad> ads, Client newPartner) {
        if (ValidateDataUtils.isNullOrEmpty(request.getAds())) {
            return true;
        }

        UUID partnerId = newPartner.getId();

        Set<UUID> partnerAdIds = ads.stream()
                .filter(ad -> ad.getClient().isPartner()
                        && Objects.equals(ad.getClient().getId(), partnerId))
                .map(Ad::getId)
                .collect(Collectors.toSet());

        int totalNonPartnerBlockQuantity = request.getAds().stream()
                .filter(dto -> !partnerAdIds.contains(dto.getId()))
                .mapToInt(MonitorAdRequestDto::getBlockQuantity)
                .sum();

        return totalNonPartnerBlockQuantity <= (SharedConstants.MAX_MONITOR_ADS - SharedConstants.PARTNER_RESERVED_SLOTS);
    }

    private void addPartnerAdsToRequest(Client partner, MonitorRequestDto request, List<Ad> allAds) {
        List<Ad> partnerAds = partner.getApprovedAds().stream()
                .filter(ad -> allAds.stream().anyMatch(a -> Objects.equals(a.getId(), ad.getId())))
                .toList();

        if (partnerAds.isEmpty()) {
            return;
        }

        Map<UUID, Integer> blockQuantities = distributePartnerBlockQuantities(partnerAds);

        AtomicInteger maxOrderIndex = new AtomicInteger(request.getAds().stream()
                .mapToInt(MonitorAdRequestDto::getOrderIndex)
                .max()
                .orElse(0));

        partnerAds.forEach(ad -> {
            MonitorAdRequestDto dto = new MonitorAdRequestDto();
            dto.setId(ad.getId());
            dto.setOrderIndex(maxOrderIndex.incrementAndGet());
            dto.setBlockQuantity(blockQuantities.get(ad.getId()));
            request.getAds().add(dto);
        });
    }

    private Map<UUID, Integer> distributePartnerBlockQuantities(List<Ad> partnerAds) {
//        Map<UUID, Integer> blockQuantities = new HashMap<>();
//
//        if (partnerAds.isEmpty()) {
//            return blockQuantities;
//        }
//
//        int adsCount = partnerAds.size();
//        int totalSlots = SharedConstants.PARTNER_RESERVED_SLOTS;
//
//        if (adsCount == 1) {
//            blockQuantities.put(partnerAds.get(0).getId(), totalSlots);
//        } else if (adsCount == 2) {
//            // Distribuição equilibrada: 4 e 3
//            blockQuantities.put(partnerAds.get(0).getId(), 4);
//            blockQuantities.put(partnerAds.get(1).getId(), 3);
//        } else if (adsCount == 3) {
//            // Distribuição equilibrada: 3, 2, 2
//            blockQuantities.put(partnerAds.get(0).getId(), 3);
//            blockQuantities.put(partnerAds.get(1).getId(), 2);
//            blockQuantities.put(partnerAds.get(2).getId(), 2);
//        } else if (adsCount == 4) {
//            // Distribuição equilibrada: 2, 2, 2, 1
//            blockQuantities.put(partnerAds.get(0).getId(), 2);
//            blockQuantities.put(partnerAds.get(1).getId(), 2);
//            blockQuantities.put(partnerAds.get(2).getId(), 2);
//            blockQuantities.put(partnerAds.get(3).getId(), 1);
//        } else if (adsCount == 5) {
//            // Distribuição equilibrada: 2, 2, 1, 1, 1
//            blockQuantities.put(partnerAds.get(0).getId(), 2);
//            blockQuantities.put(partnerAds.get(1).getId(), 2);
//            blockQuantities.put(partnerAds.get(2).getId(), 1);
//            blockQuantities.put(partnerAds.get(3).getId(), 1);
//            blockQuantities.put(partnerAds.get(4).getId(), 1);
//        } else if (adsCount == 6) {
//            // Distribuição equilibrada: 2, 1, 1, 1, 1, 1
//            blockQuantities.put(partnerAds.get(0).getId(), 2);
//            for (int i = 1; i < adsCount; i++) {
//                blockQuantities.put(partnerAds.get(i).getId(), 1);
//            }
//        } else if (adsCount == 7) {
//            // Todos recebem 1
//            partnerAds.forEach(ad -> blockQuantities.put(ad.getId(), 1));
//        } else {
//            // Para mais de 7 ads (caso raro), distribuir de forma equilibrada
//            // Base: cada um recebe pelo menos 1, e os primeiros recebem o restante distribuído
//            int baseQuantity = totalSlots / adsCount;
//            int remainder = totalSlots % adsCount;
//
//            for (int i = 0; i < adsCount; i++) {
//                int quantity = baseQuantity + (i < remainder ? 1 : 0);
//                blockQuantities.put(partnerAds.get(i).getId(), quantity);
//            }
//        }
//
//        return blockQuantities;


        if (partnerAds == null || partnerAds.isEmpty()) {
            return Collections.emptyMap();
        }

        int adsCount = partnerAds.size();
        int capacity = (int) (adsCount / 0.75f) + 1;
        Map<UUID, Integer> blockQuantities = new HashMap<>(capacity);

        // Distribuições pré-definidas para 1..7 anúncios
        final int[][] distributions = {
                {}, // índice 0 não usado
                {7},
                {4, 3},
                {3, 2, 2},
                {2, 2, 2, 1},
                {2, 2, 1, 1, 1},
                {2, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1}
        };

        int[] dist = distributions[adsCount];
        IntStream.range(0, adsCount).forEach(i -> blockQuantities.put(partnerAds.get(i).getId(), dist[i]));
        return blockQuantities;
    }

    /**
     * Valida e ajusta blockQuantity conforme as regras:
     * - Só pode ter 1 ad com blockQuantity = 7
     * - Os outros devem ter blockQuantity = 1 ou 2
     * - Soma total <= MAX_MONITOR_ADS
     */
    private void validateAndAdjustBlockQuantities(MonitorRequestDto request, Monitor monitor, List<Ad> ads) {
        if (ValidateDataUtils.isNullOrEmpty(request.getAds())) {
            return;
        }

        UUID partnerId = monitor.getAddress() != null && monitor.getAddress().getClient() != null
                ? monitor.getAddress().getClient().getId()
                : null;

        List<MonitorAdRequestDto> partnerAds = new ArrayList<>();
        List<MonitorAdRequestDto> otherAds = new ArrayList<>();

        request.getAds().forEach(dto -> {
            Ad ad = ads.stream()
                    .filter(a -> Objects.equals(a.getId(), dto.getId()))
                    .findFirst()
                    .orElse(null);

            if (ad != null && ad.getClient().isPartner() && Objects.equals(ad.getClient().getId(), partnerId)) {
                partnerAds.add(dto);
            } else {
                otherAds.add(dto);
            }
        });

//        validateAdsLimit(monitor, ads);
    }

    private void ensureSequentialOrderIndex(List<Ad> ads,
                                            Map<UUID, MonitorAd> existingAfterRemoval,
                                            Map<UUID, MonitorAd> newMonitorAdsByAdId,
                                            Map<UUID, MonitorAdRequestDto> adRequestMap) {
        // Ordenar ads por orderIndex atual
        List<Ad> sortedAds = ads.stream()
                .sorted(Comparator.comparing(ad -> {
                    MonitorAd ma = existingAfterRemoval.getOrDefault(ad.getId(), newMonitorAdsByAdId.get(ad.getId()));
                    MonitorAdRequestDto dto = adRequestMap.get(ad.getId());
                    if (dto != null && dto.getOrderIndex() != null) {
                        return dto.getOrderIndex();
                    }
                    if (ma != null && ma.getOrderIndex() != null) {
                        return ma.getOrderIndex();
                    }
                    return Integer.MAX_VALUE;
                }))
                .toList();

        // Atualizar orderIndex sequencialmente
        int orderIndex = 1;
        for (Ad ad : sortedAds) {
            MonitorAd ma = existingAfterRemoval.getOrDefault(ad.getId(), newMonitorAdsByAdId.get(ad.getId()));
            MonitorAdRequestDto dto = adRequestMap.get(ad.getId());

            if (ma != null) {
                ma.setOrderIndex(orderIndex);
            }
            if (dto != null) {
                dto.setOrderIndex(orderIndex);
            }
            orderIndex++;
        }
    }
}
