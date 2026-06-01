package com.telas.services.monitor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.request.filters.FilterMonitorRequestDto;
import com.telas.dtos.response.*;
import com.telas.entities.*;
import com.telas.helpers.MonitorHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdRepository;
import com.telas.repositories.MonitorAdRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.AdUnusedTrackingService;
import com.telas.services.MonitorSubscriptionService;
import com.telas.services.RemoveMonitorAdsOutcome;
import com.telas.services.SubscriptionService;
import com.telas.services.impl.UnusedSingleAdDeletionService;
import com.telas.services.partner.PartnerPlacementRules;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.valitation.AuthValidationMessageConstants;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitorCrudService {

	private final Logger log = LoggerFactory.getLogger(MonitorCrudService.class);

	private final AuthenticatedUserService authenticatedUserService;
	private final MonitorRepository repository;
	private final AdRepository adRepository;
	private final MonitorAdRepository monitorAdRepository;
	private final MonitorHelper helper;
	private final SubscriptionService subscriptionService;
	private final MonitorSubscriptionService monitorSubscriptionService;
	private final UnusedSingleAdDeletionService unusedSingleAdDeletionService;
	private final AdUnusedTrackingService adUnusedTrackingService;
	private final PartnerPlacementRules partnerPlacementRules;
	private final MonitorPlaylistService monitorPlaylistService;
	private final MonitorAdsQuotaRules monitorAdsQuotaRules;

	@Value("${stripe.product.id}")
	private String productId;

	@Transactional
	public UUID save(MonitorRequestDto request, UUID monitorId) throws JsonProcessingException {
		AuthenticatedUser authenticatedUser = authenticatedUserService.getLoggedUser();
		validateSaveAccess(monitorId, authenticatedUser);
		request.validate();
		Address address = helper.getAddress(request);

		if (monitorId != null) {
			validateAddressAvailability(address, monitorId);
			final List<Ad> ads;
			if (request.getAds() == null) {
				Monitor current = findEntityById(monitorId);
				ads = new ArrayList<>(current.getAds());
			} else if (request.getAds().isEmpty()) {
				ads = List.of();
			} else {
				ads = helper.getAds(request, monitorId);
			}
			updateExistingMonitor(request, monitorId, authenticatedUser, address, ads);
			return null;
		}

		validateAddressAvailability(address);
		Monitor created = createNewMonitor(authenticatedUser, address);
		return created.getId();
	}

	@Transactional
	public RemoveMonitorAdsOutcome removeMonitorAdsFromSubscription(Subscription subscription) {
		return monitorSubscriptionService.removeMonitorAdsFromSubscription(subscription);
	}

	@Transactional(readOnly = true)
	public MonitorResponseDto findById(UUID monitorId) {
		Monitor entity = repository.findById(monitorId)
			.orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));

		List<MonitorAdResponseDto> adLinks = helper.getMonitorAdsResponse(entity);

		return new MonitorResponseDto(entity, adLinks, adRepository.countAllApprovedNotInMonitor(entity.getId()));
	}

	@Transactional
	public Monitor findEntityById(UUID monitorId) {
		return repository.findById(monitorId)
			.orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public List<MonitorsBoxMinResponseDto> findAllMonitors() {
		return repository.findAll().stream().map(MonitorsBoxMinResponseDto::new).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<MonitorValidAdResponseDto> findValidAdsForMonitor(UUID monitorId, String name) {
		authenticatedUserService.validateAdmin();
		Monitor monitor = findEntityById(monitorId);
		return helper.getValidAdsForMonitor(monitor, name);
	}

	@Transactional(readOnly = true)
	public PaginationResponseDto<List<MonitorResponseDto>> findAllByFilters(FilterMonitorRequestDto request) {
		authenticatedUserService.validateAdmin();

		Sort order = request.setOrdering();
		Pageable pageable = PaginationFilterUtil.getPageable(request, order);
		Specification<Monitor> filter = PaginationFilterUtil.addSpecificationFilter(null, request.getGenericFilter(),
			this::filterMonitors);

		Page<Monitor> page = repository.findAll(filter, pageable);
		List<MonitorResponseDto> response = page.stream()
			.map(monitor -> new MonitorResponseDto(
				monitor,
				helper.getMonitorAdsResponse(monitor),
				adRepository.countAllApprovedNotInMonitor(monitor.getId())
			)).toList();

		return PaginationResponseDto.fromResult(response, (int) page.getTotalElements(), page.getTotalPages(),
			request.getPage());
	}

	@Transactional
	public void deleteAvailableAd(UUID monitorId, UUID adId) {
		authenticatedUserService.validateAdmin();
		Ad ad = adRepository.findById(adId)
				.orElseThrow(() -> new ResourceNotFoundException("Ad not found"));

		if (ad.getMonitorAds() != null && !ad.getMonitorAds().isEmpty()) {
			monitorAdRepository.deleteByAdId(adId);
		}
		unusedSingleAdDeletionService.deleteAdInNewTransaction(adId);
	}

	@Transactional
	public void delete(UUID monitorId) {
		authenticatedUserService.validateAdmin();
		Monitor monitor = findEntityById(monitorId);
		ensureNoActiveSubscription(monitor);
		Set<UUID> adIds =
				monitor.getMonitorAds().stream().map(ma -> ma.getAd().getId()).collect(Collectors.toSet());
		clearMonitorAssociations(monitor);
		repository.delete(monitor);
		adUnusedTrackingService.syncUnusedStateForAdIds(adIds);
	}

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

	public void validateMonitorAdsQuotas(Monitor monitor, List<Ad> ads) {
		monitorAdsQuotaRules.validateMonitorAdsQuotas(monitor, ads);
	}

	private void validateSaveAccess(UUID monitorId, AuthenticatedUser user) {
		Client c = user.client();
		if (c.isAdmin() || c.isDeveloper()) {
			return;
		}
		if (monitorId == null) {
			throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
		}
		Monitor monitor = findEntityById(monitorId);
		if (c.isPartner() && partnerPlacementRules.partnerOwnsMonitorAddress(c, monitor)) {
			return;
		}
		if (repository.hasActiveSubscriptionForClientAndMonitor(c.getId(), monitorId)) {
			return;
		}
		throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
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

			String filterKey = genericFilter.toLowerCase();
			if ("active".equals(filterKey)) {
				predicates.add(criteriaBuilder.equal(root.get("active"), true));
			} else if ("inactive".equals(filterKey)) {
				predicates.add(criteriaBuilder.equal(root.get("active"), false));
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

	private void updateExistingMonitor(MonitorRequestDto request, UUID monitorId, AuthenticatedUser authenticatedUser,
		Address address, List<Ad> ads) {
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

		boolean hasSpaceForPartner = monitorPlaylistService.hasSpaceForPartnerAds(request, ads, newPartner);

		List<Ad> updatedAds = hasSpaceForPartner ? addNewPartnerAds(ads, newPartner) : ads;

		if (hasSpaceForPartner && !ValidateDataUtils.isNullOrEmpty(newPartner.getApprovedAds())) {
			monitorPlaylistService.addPartnerAdsToRequest(newPartner, request, updatedAds);
		}

		return updatedAds;
	}

	private List<Ad> addNewPartnerAds(List<Ad> ads, Client newPartner) {
		if (newPartner == null || ValidateDataUtils.isNullOrEmpty(newPartner.getApprovedAds())) {
			return ads;
		}

		List<Ad> mutable = ValidateDataUtils.isNullOrEmpty(ads) ? new ArrayList<>() : new ArrayList<>(ads);
		Set<UUID> adIds = mutable.stream().map(Ad::getId).filter(Objects::nonNull).collect(Collectors.toSet());

		newPartner.getApprovedAds().stream().filter(ad -> ad != null && ad.getId() != null && !adIds.contains(ad.getId()))
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
			UUID partnerId = newPartner.getId();
			UUID monitorId = monitor.getId();
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						subscriptionService.schedulePartnerBonusAfterMonitorCommit(partnerId, monitorId);
					}
				});
			} else {
				subscriptionService.schedulePartnerBonusAfterMonitorCommit(partnerId, monitorId);
			}
		}
	}

	private void cancelPartnerBonusSubscription(Client partner) {
		try {
			subscriptionService.cancelBonusSubscription(partner);
		} catch (RuntimeException e) {
			log.error("cancelPartnerBonusSubscription failed for partner {}: {}", partner.getId(), e.getMessage(), e);
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
		monitorPlaylistService.updateMonitorAds(request, monitor, ads);
	}
}
