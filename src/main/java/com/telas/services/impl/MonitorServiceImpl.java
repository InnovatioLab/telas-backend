package com.telas.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.MonitorRequestDto;
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
import com.telas.services.MonitorService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.PaginationFilterUtil;
import com.telas.shared.utils.ValidateDataUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {
    private final AuthenticatedUserService authenticatedUserService;
    private final MonitorRepository repository;
    private final MonitorHelper helper;

    @Value("${stripe.product.id}")
    private String productId;

    @Override
    @Transactional
    public void save(MonitorRequestDto request, UUID monitorId) throws JsonProcessingException {
        AuthenticatedUser authenticatedUser = authenticatedUserService.validateAdmin();
        request.validate();
        Address address = helper.getOrCreateAddress(request);

        Monitor monitor;

        if (monitorId != null) {
            List<Ad> ads = !ValidateDataUtils.isNullOrEmpty(request.getAds()) ? helper.getAds(request, monitorId) : Collections.emptyList();
            monitor = updateExistingMonitor(request, monitorId, authenticatedUser, address, ads);
        } else {
            monitor = createNewMonitor(request, authenticatedUser, address);
        }

        repository.save(monitor);
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
                .collect(Collectors.groupingBy(monitor -> monitor.getAddress().getCoordinatesParams()))
                .values().stream()
                .map(monitors -> {
                    List<UUID> monitorIds = monitors.stream()
                            .map(Monitor::getId)
                            .toList();
                    String fullAddress = monitors.isEmpty() ? null : monitors.get(0).getAddress().getCoordinatesParams();
                    boolean hasBox = monitors.stream().allMatch(monitor -> monitor.getBox() != null);
                    return new MonitorsBoxMinResponseDto(monitorIds, fullAddress, hasBox);
                })
                .toList();
    }


    @Override
    @Transactional
    public List<Monitor> findAllByIds(List<UUID> monitorIds) {
        return repository.findAllByIdIn(monitorIds);
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
        helper.sendBoxRemoveMonitor(monitor);
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

    private Monitor createNewMonitor(MonitorRequestDto request, AuthenticatedUser authenticatedUser, Address address) {
        helper.setAddressCoordinates(address);
        Monitor monitor = new Monitor(request, address, productId);
        monitor.setUsernameCreate(authenticatedUser.client().getBusinessName());
        return monitor;
    }

    private Monitor updateExistingMonitor(MonitorRequestDto request, UUID monitorId, AuthenticatedUser authenticatedUser, Address address, List<Ad> ads) {
        Monitor monitor = findEntityById(monitorId);

        if (!monitor.getAddress().getId().equals(address.getId())) {
            monitor.setAddress(address);

            if (!address.hasLocation()) {
                helper.setAddressCoordinates(address);
            }

        }

        String usernameUpdate = authenticatedUser.client().getBusinessName();
        CustomRevisionListener.setUsername(usernameUpdate);
        monitor.setUsernameUpdate(usernameUpdate);

        updateMonitorDetails(request, monitor, ads);
        return monitor;
    }

    private void updateMonitorDetails(MonitorRequestDto request, Monitor monitor, List<Ad> ads) {
        monitor.setProductId(productId);
        monitor.setLocationDescription(request.getLocationDescription());
        monitor.setActive(request.getActive() != null ? request.getActive() : monitor.isActive());

        if (ValidateDataUtils.isNullOrEmpty(ads)) {
            return;
        }

        updateMonitorAds(request, monitor, ads);
    }

    private void updateMonitorAds(MonitorRequestDto request, Monitor monitor, List<Ad> ads) {
        monitor.getMonitorAds().clear();

        if (!monitor.isWithinAdsLimit(ads.size())) {
            throw new BusinessRuleException(MonitorValidationMessages.ADS_LIMIT_EXCEEDED + monitor.getMaxBlocks());
        }

        List<MonitorAd> monitorAds = IntStream.range(0, ads.size())
                .mapToObj(i -> new MonitorAd(request.getAds().get(i), monitor, ads.get(i)))
                .toList();

        monitor.getMonitorAds().addAll(monitorAds);

        if (monitor.isAbleToSendBoxRequest()) {
            helper.sendBoxesMonitorsUpdateAds(monitor, ads);
        }
    }
}
