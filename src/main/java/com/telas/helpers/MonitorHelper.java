package com.telas.helpers;

import com.telas.dtos.request.MonitorAdRequestDto;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.dtos.response.MonitorValidAdResponseDto;
import com.telas.entities.Ad;
import com.telas.entities.Address;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.entities.SubscriptionMonitor;
import com.telas.enums.AdValidationType;
import com.telas.entities.Client;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.repositories.SubscriptionMonitorRepository;
import com.telas.services.AddressService;
import com.telas.services.MapsService;
import com.telas.services.box.BoxPlaylistClient;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MonitorHelper {

	private final Logger log = LoggerFactory.getLogger(MonitorHelper.class);

	private final MapsService mapsService;

	private final AdRepository adRepository;

	private final MonitorRepository repository;

	private final SubscriptionMonitorRepository subscriptionMonitorRepository;

	private final AddressService addressService;

	private final MonitorAdDtoMapper monitorAdDtoMapper;

	private final BoxPlaylistClient boxPlaylistClient;

	private final AuthenticatedUserService authenticatedUserService;

	@Transactional
	public List<Ad> getAds(MonitorRequestDto request, UUID monitorId) {
		Set<UUID> adsIds = request.getAds().stream().map(MonitorAdRequestDto::getId).collect(Collectors.toSet());
		if (adsIds.isEmpty()) {
			return Collections.emptyList();
		}

		Client actor = authenticatedUserService.getLoggedUser().client();
		if (actor.isPrivilegedPanelUser()) {
			List<Ad> approvedAds = adRepository.findApprovedNonPdfByIds(adsIds);
			Set<UUID> approvedIds = approvedAds.stream().map(Ad::getId).collect(Collectors.toSet());
			if (!approvedIds.containsAll(adsIds)) {
				throw new BusinessRuleException(MonitorValidationMessages.AD_NOT_ABLE_TO_ASSIGN_TO_MONITOR);
			}
			return approvedAds;
		}

		List<Ad> assignableAds = adRepository.findAllValidAdsForMonitor(AdValidationType.APPROVED, monitorId);
		Set<UUID> assignableIds = assignableAds.stream().map(Ad::getId).collect(Collectors.toSet());
		if (!assignableIds.containsAll(adsIds)) {
			throw new BusinessRuleException(MonitorValidationMessages.AD_NOT_ABLE_TO_ASSIGN_TO_MONITOR);
		}

		return assignableAds.stream().filter(ad -> adsIds.contains(ad.getId())).collect(Collectors.toList());
	}

	@Transactional
	public void setAddressCoordinates(Address address) {
		mapsService.getAddressCoordinates(address);
		addressService.save(address);
	}

	@Transactional(readOnly = true)
	public List<MonitorValidAdResponseDto> getValidAdsForMonitor(Monitor monitor, String name) {
		return monitorAdDtoMapper.toValidAdDtos(adRepository.findAllApprovedNotInMonitorFiltered(monitor.getId(), name));
	}

	@Transactional(readOnly = true)
	public List<MonitorAdResponseDto> getPartnerAdvertiserAdsOnMonitor(Monitor entity, UUID partnerId) {
		return monitorAdDtoMapper.toPartnerAdvertiserAdsOnMonitor(
				entity,
				partnerId,
				loadActiveSubscriptionByClientId(entity.getId())
		);
	}

	@Transactional(readOnly = true)
	public List<MonitorAdResponseDto> getPartnerAdvertiserAdsForPortal(Monitor entity, UUID partnerId) {
		if (partnerId == null || entity == null) {
			return List.of();
		}
		List<MonitorAdResponseDto> result = new ArrayList<>();
		Set<UUID> coveredAdIds = new HashSet<>();

		if (entity.getMonitorAds() != null) {
			for (MonitorAd monitorAd : entity.getMonitorAds()) {
				Ad ad = monitorAd.getAd();
				if (ad == null || ad.getClient() == null || !partnerId.equals(ad.getClient().getId())) {
					continue;
				}
				if (!AdValidationType.APPROVED.equals(ad.getValidation())) {
					continue;
				}
				coveredAdIds.add(ad.getId());
				result.add(monitorAdDtoMapper.toPartnerPortalAdDto(monitorAd, ad));
			}
		}

		List<Ad> pendingOnMonitor = adRepository.findApprovedPartnerAdsPendingPlaylistOnMonitor(
				partnerId, entity.getId());
		for (Ad ad : pendingOnMonitor) {
			if (coveredAdIds.add(ad.getId())) {
				result.add(monitorAdDtoMapper.toPartnerPortalAdDto(null, ad));
			}
		}
		return result;
	}

	@Transactional(readOnly = true)
	public List<MonitorAdResponseDto> getMonitorAdsResponse(Monitor entity) {
		return monitorAdDtoMapper.toMonitorAdsResponse(entity, loadActiveSubscriptionByClientId(entity.getId()));
	}

	@Transactional(readOnly = true)
	public Predicate createAddressPredicate(CriteriaBuilder criteriaBuilder, Root<Monitor> root, String filter) {
		return criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat(criteriaBuilder.concat(
				criteriaBuilder.concat(criteriaBuilder.concat(root.get("address").get("street"), " "),
					root.get("address").get("city")), " "),
			criteriaBuilder.concat(criteriaBuilder.concat(root.get("address").get("state"), " "),
				root.get("address").get("zipCode")))), filter);
	}

	@Transactional(readOnly = true)
	public List<UpdateBoxMonitorsAdRequestDto> buildOrderedBoxUpdateDtos(Monitor monitor) {
		if (monitor.getMonitorAds() == null || monitor.getMonitorAds().isEmpty()) {
			return List.of();
		}
		return monitor.getMonitorAds().stream()
				.filter(ma -> ma.getAd() != null
						&& ma.getMonitor() != null
						&& ma.getMonitor().getBox() != null
						&& ma.getMonitor().getBox().getBoxAddress() != null)
				.sorted(Comparator.comparingInt(ma -> Optional.ofNullable(ma.getOrderIndex()).orElse(0)))
				.map(ma -> new UpdateBoxMonitorsAdRequestDto(ma.getAd(), ma,
						monitorAdDtoMapper.getAdLink(ma.getAd())))
				.toList();
	}

	@Transactional
	public void detachAdFromMonitor(Monitor monitor, UUID adId) {
		if (monitor == null || monitor.getMonitorAds() == null || adId == null) {
			return;
		}
		monitor.getMonitorAds().removeIf(ma -> ma.getAd() != null && adId.equals(ma.getAd().getId()));
		repository.save(monitor);
	}

	@Transactional
	public void attachAdToMonitor(Monitor monitor, Ad ad) {
		if (monitor == null || ad == null || monitor.getMonitorAds() == null) {
			return;
		}
		boolean alreadyLinked = monitor.getMonitorAds().stream()
				.anyMatch(ma -> ma.getAd() != null && ad.getId().equals(ma.getAd().getId()));
		if (!alreadyLinked) {
			monitor.getMonitorAds().add(new MonitorAd(monitor, ad));
			repository.save(monitor);
		}
	}

	public boolean stageAdFileOnBox(Monitor monitor, Ad ad) {
		if (monitor == null || ad == null || !monitor.isAbleToSendBoxRequest()) {
			return false;
		}
		String baseUrl = boxPlaylistClient.resolveBoxBaseUrl(monitor.getBox().getBoxAddress().getIp());
		if (baseUrl == null) {
			return false;
		}
		UpdateBoxMonitorsAdRequestDto dto = new UpdateBoxMonitorsAdRequestDto();
		dto.setFileName(ad.getName());
		dto.setLink(monitorAdDtoMapper.getAdLink(ad));
		dto.setBlockQuantity(1);
		dto.setOrderIndex(0);
		dto.setBaseUrl(baseUrl);
		boolean staged = boxPlaylistClient.stageAdFile(baseUrl, dto);
		if (!staged) {
			log.error("Error staging ad file on box monitorId={}, adId={}", monitor.getId(), ad.getId());
		}
		return staged;
	}

	public Set<String> syncBoxAdsPlaylist(Monitor monitor, List<UpdateBoxMonitorsAdRequestDto> requestList) {
		Set<String> successfulBaseUrls = new HashSet<>();
		if (monitor == null || monitor.getBox() == null || monitor.getBox().getBoxAddress() == null) {
			return successfulBaseUrls;
		}
		String baseUrl = boxPlaylistClient.resolveBoxBaseUrl(monitor.getBox().getBoxAddress().getIp());
		if (baseUrl == null) {
			return successfulBaseUrls;
		}
		List<UpdateBoxMonitorsAdRequestDto> items = requestList != null ? requestList : List.of();
		if (items.isEmpty()) {
			if (boxPlaylistClient.pushEmptyPlaylist(baseUrl, monitor.getId())) {
				successfulBaseUrls.add(baseUrl);
			}
			return successfulBaseUrls;
		}
		return boxPlaylistClient.pushPlaylistUpdates(items);
	}

	public Set<String> sendBoxesMonitorsUpdateAdsReturnSuccess(List<UpdateBoxMonitorsAdRequestDto> requestList) {
		return boxPlaylistClient.pushPlaylistUpdates(requestList);
	}

	public void sendBoxesMonitorsRemoveAds(Monitor monitor, List<String> adNamesToRemove) {
		if (monitor == null || monitor.getBox() == null || monitor.getBox().getBoxAddress() == null) {
			return;
		}
		log.info("Sending request to remove ads from boxMonitorsAds for monitor with ID: {}", monitor.getId());
		boxPlaylistClient.pushRemoveAds(monitor.getBox().getBoxAddress().getIp(), adNamesToRemove);
	}

	@Transactional
	public void sendBoxesMonitorsRemoveAd(Ad ad, List<String> adNameToRemove) {
		List<Monitor> activeMonitorsToUpdate = getClientMonitorsWithActiveSubscription(ad.getClient().getId()).stream()
			.filter(monitor -> monitor.getMonitorAds().stream()
				.anyMatch(monitorAd -> monitorAd.getAd().getId().equals(ad.getId()))).toList();

		activeMonitorsToUpdate.forEach(monitor -> {
			monitor.getMonitorAds().removeIf(monitorAd -> monitorAd.getAd().getId().equals(ad.getId()));
			repository.save(monitor);
		});

		activeMonitorsToUpdate.stream().filter(Monitor::isAbleToSendBoxRequest)
			.forEach(monitor -> sendBoxesMonitorsRemoveAds(monitor, adNameToRemove));
	}

	@Transactional(readOnly = true)
	public List<MonitorValidAdResponseDto> getBoxMonitorAdsResponse(Monitor monitor, List<String> adNames) {
		return monitorAdDtoMapper.toBoxMonitorAdsResponse(monitor, adNames);
	}

	@Transactional(readOnly = true)
	public List<String> getCurrentDisplayedAdsFromBox(Monitor monitor) {
		if (!monitor.isAbleToSendBoxRequest()) {
			return Collections.emptyList();
		}
		return boxPlaylistClient.getCurrentDisplayedAds(
				monitor.getBox().getBoxAddress().getIp(),
				monitor.getId()
		);
	}

	private List<Monitor> getClientMonitorsWithActiveSubscription(UUID clientId) {
		return repository.findMonitorsWithActiveSubscriptionsByClientId(clientId);
	}

	@Transactional
	public List<SubscriptionMonitor> getSubscriptionsMonitorsFromMonitor(UUID id) {
		return subscriptionMonitorRepository.findByMonitorId(id);
	}

	@Transactional
	public Address getAddress(MonitorRequestDto request) {
		Address address = (request.getAddressId() != null)
			? addressService.findById(request.getAddressId())
			: addressService.getPartnerAddress(request.getAddress());

		if (!address.getClient().isPartner()) {
			throw new BusinessRuleException(MonitorValidationMessages.CLIENT_NOT_PARTNER);
		}

		return address;
	}

	private Map<UUID, SubscriptionMonitor> loadActiveSubscriptionByClientId(UUID monitorId) {
		return subscriptionMonitorRepository.findByMonitorId(monitorId)
				.stream()
				.collect(Collectors.toMap(
						sm -> sm.getId().getSubscription().getClient().getId(),
						sm -> sm,
						(a, b) -> a
				));
	}
}
