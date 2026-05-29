package com.telas.services.monitor;

import com.telas.dtos.request.MonitorAdRequestDto;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.request.UpdateBoxMonitorsAdRequestDto;
import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.entities.SubscriptionMonitor;
import com.telas.helpers.AdMediaLinkFactory;
import com.telas.helpers.AdPublicationNotificationHelper;
import com.telas.helpers.MonitorHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.services.AdUnusedTrackingService;
import com.telas.services.PartnerSlotAccessService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MonitorPlaylistService {

	private final MonitorHelper helper;
	private final AdPublicationNotificationHelper adPublicationNotificationHelper;
	private final AdMediaLinkFactory adMediaLinkFactory;
	private final AdUnusedTrackingService adUnusedTrackingService;
	private final PartnerSlotAccessService partnerSlotAccessService;
	private final MonitorAdsQuotaRules monitorAdsQuotaRules;

	public void updateMonitorAds(MonitorRequestDto request, Monitor monitor, List<Ad> ads) {
		monitorAdsQuotaRules.validateMonitorAdsQuotas(monitor, ads);
		Set<UUID> newAdIds = buildNewAdIds(ads);
		Set<UUID> removedAdIds =
				monitor.getMonitorAds().stream()
						.filter(ma -> !newAdIds.contains(ma.getAd().getId()))
						.map(ma -> ma.getAd().getId())
						.collect(Collectors.toSet());
		removeStaleMonitorAds(monitor, newAdIds);

		Map<UUID, MonitorAdRequestDto> adRequestMap = mapAdsById(request);
		Map<UUID, MonitorAd> existingAfterRemoval = buildExistingAfterRemoval(monitor);
		Map<UUID, SubscriptionMonitor> subscriptionByClientId = buildSubscriptionByClientId(monitor);

		List<MonitorAd> newMonitorAds = createNewMonitorAds(ads, existingAfterRemoval, adRequestMap, monitor);
		updateOrderIndexes(ads, existingAfterRemoval, adRequestMap);

		Map<UUID, MonitorAd> newMonitorAdsByAdId = newMonitorAds.stream()
			.collect(Collectors.toMap(ma -> ma.getAd().getId(), ma -> ma));

		Map<UUID, Integer> blockQuantities =
				resolveBlockQuantities(ads, adRequestMap, subscriptionByClientId, monitor);
		applyBlockQuantities(existingAfterRemoval, newMonitorAdsByAdId, blockQuantities);
		validatePartnerBlockTotalsOnMonitor(monitor);

		List<UpdateBoxMonitorsAdRequestDto> requestList = buildRequestList(ads, existingAfterRemoval, newMonitorAdsByAdId);

		addNewMonitorAdsToMonitor(monitor, newMonitorAds);

		Set<String> successfulBaseUrls = new HashSet<>();
		if (monitor.isAbleToSendBoxRequest()) {
			successfulBaseUrls = helper.syncBoxAdsPlaylist(monitor, requestList);
		}
		if (!newMonitorAds.isEmpty()) {
			adPublicationNotificationHelper.notifyAfterPlaylistUpdate(
					monitor, newMonitorAds, successfulBaseUrls, requestList);
		}

		Set<UUID> toSync = new HashSet<>(newAdIds);
		toSync.addAll(removedAdIds);
		adUnusedTrackingService.syncUnusedStateForAdIds(toSync);
	}

	public Set<String> syncBoxAdsPlaylist(Monitor monitor, List<UpdateBoxMonitorsAdRequestDto> requestList) {
		return helper.syncBoxAdsPlaylist(monitor, requestList);
	}

	public Map<UUID, Integer> distributePartnerBlockQuantities(List<Ad> partnerAds) {
		if (partnerAds == null || partnerAds.isEmpty()) {
			return Collections.emptyMap();
		}

		int adsCount = partnerAds.size();
		int capacity = (int) (adsCount / 0.75f) + 1;
		Map<UUID, Integer> blockQuantities = new HashMap<>(capacity);

		final int[][] distributions = {{}, {5}, {3, 2}, {2, 2, 1}, {2, 1, 1, 1}, {1, 1, 1, 1, 1}};

		int[] dist = distributions[adsCount];
		IntStream.range(0, adsCount).forEach(i -> blockQuantities.put(partnerAds.get(i).getId(), dist[i]));
		return blockQuantities;
	}

	public void addPartnerAdsToRequest(Client partner, MonitorRequestDto request, List<Ad> allAds) {
		if (request.getAds() == null) {
			request.setAds(new ArrayList<>());
		}
		List<Ad> partnerAds = partner.getApprovedAds().stream()
			.filter(ad -> allAds.stream().anyMatch(a -> Objects.equals(a.getId(), ad.getId()))).toList();

		if (partnerAds.isEmpty()) {
			return;
		}

		Map<UUID, Integer> blockQuantities = distributePartnerBlockQuantities(partnerAds);

		AtomicInteger maxOrderIndex = new AtomicInteger(
			request.getAds().stream().mapToInt(MonitorAdRequestDto::getOrderIndex).max().orElse(0));

		partnerAds.forEach(ad -> {
			MonitorAdRequestDto dto = new MonitorAdRequestDto();
			dto.setId(ad.getId());
			dto.setOrderIndex(maxOrderIndex.incrementAndGet());
			dto.setBlockQuantity(blockQuantities.get(ad.getId()));
			request.getAds().add(dto);
		});
	}

	public boolean hasSpaceForPartnerAds(MonitorRequestDto request, List<Ad> ads, Client newPartner) {
		if (newPartner == null || !newPartner.isPartner() || ValidateDataUtils.isNullOrEmpty(request.getAds())) {
			return true;
		}

		UUID partnerId = newPartner.getId();

		Set<UUID> partnerAdIds = ads.stream()
			.filter(ad -> ad.getClient() != null && ad.getClient().isPartner() && Objects.equals(ad.getClient().getId(),
				partnerId)).map(Ad::getId).collect(Collectors.toSet());

		int totalNonPartnerBlockQuantity = request.getAds().stream().filter(dto -> !partnerAdIds.contains(dto.getId()))
			.mapToInt(MonitorAdRequestDto::getBlockQuantity).sum();

		return totalNonPartnerBlockQuantity <= (SharedConstants.MAX_MONITOR_ADS - SharedConstants.PARTNER_RESERVED_SLOTS);
	}

	private Set<UUID> buildNewAdIds(List<Ad> ads) {
		return ads.stream().map(Ad::getId).collect(Collectors.toSet());
	}

	private void removeStaleMonitorAds(Monitor monitor, Set<UUID> newAdIds) {
		monitor.getMonitorAds().removeIf(ma -> !newAdIds.contains(ma.getAd().getId()));
	}

	private Map<UUID, MonitorAd> buildExistingAfterRemoval(Monitor monitor) {
		return monitor.getMonitorAds().stream().collect(Collectors.toMap(ma -> ma.getAd().getId(), ma -> ma));
	}

	private Map<UUID, SubscriptionMonitor> buildSubscriptionByClientId(Monitor monitor) {
		List<SubscriptionMonitor> subscriptionMonitors = helper.getSubscriptionsMonitorsFromMonitor(monitor.getId());
		return subscriptionMonitors.stream()
			.filter(sm -> sm.getSubscription() != null && sm.getSubscription().getClient() != null
				&& sm.getSubscription().getClient().getId() != null)
			.collect(Collectors.toMap(sm -> sm.getSubscription().getClient().getId(), sm -> sm, (a, b) -> a));
	}

	private List<MonitorAd> createNewMonitorAds(List<Ad> ads, Map<UUID, MonitorAd> existingAfterRemoval,
		Map<UUID, MonitorAdRequestDto> adRequestMap, Monitor monitor) {
		return ads.stream().filter(ad -> !existingAfterRemoval.containsKey(ad.getId())).map(ad -> {
			MonitorAdRequestDto reqDto = adRequestMap.get(ad.getId());
			return reqDto != null ? new MonitorAd(reqDto, monitor, ad) : new MonitorAd(monitor, ad);
		}).toList();
	}

	private void updateOrderIndexes(List<Ad> ads, Map<UUID, MonitorAd> existingAfterRemoval,
		Map<UUID, MonitorAdRequestDto> adRequestMap) {
		ads.stream().filter(ad -> existingAfterRemoval.containsKey(ad.getId())).forEach(ad -> {
			MonitorAd ma = existingAfterRemoval.get(ad.getId());
			MonitorAdRequestDto dto = adRequestMap.get(ad.getId());
			if (ma != null && dto != null) {
				ma.setOrderIndex(dto.getOrderIndex());
			}
		});
	}

	private Map<UUID, Integer> resolveBlockQuantities(
			List<Ad> ads,
			Map<UUID, MonitorAdRequestDto> adRequestMap,
			Map<UUID, SubscriptionMonitor> subscriptionByClientId,
			Monitor monitor) {
		Map<UUID, Integer> blockQuantities = new HashMap<>(ads.size());

		ads.forEach(ad -> {
			UUID adId = ad.getId();
			UUID clientId = ad.getClient() != null ? ad.getClient().getId() : null;
			SubscriptionMonitor matched = clientId != null ? subscriptionByClientId.get(clientId) : null;

			Integer blockQuantity = (matched != null && matched.getSlotsQuantity() != null
				&& matched.getSlotsQuantity() != SharedConstants.PARTNER_RESERVED_SLOTS)
				? matched.getSlotsQuantity()
				: Optional.ofNullable(adRequestMap.get(adId)).map(MonitorAdRequestDto::getBlockQuantity)
					.orElse(SharedConstants.MIN_QUANTITY_MONITOR_BLOCK);

			blockQuantities.put(adId, blockQuantity);
		});

		Map<UUID, List<Ad>> partnerQuotaAdsByClient =
				ads.stream()
						.filter(
								ad ->
										ad.getClient() != null
												&& partnerSlotAccessService.usesPartnerQuotaOnMonitor(
														ad.getClient(), monitor))
						.collect(Collectors.groupingBy(ad -> ad.getClient().getId()));

		partnerQuotaAdsByClient
				.values()
				.forEach(clientAds -> blockQuantities.putAll(distributePartnerBlockQuantities(clientAds)));

		return blockQuantities;
	}

	private void validatePartnerBlockTotalsOnMonitor(Monitor monitor) {
		Map<UUID, Integer> totalsByClient = new HashMap<>();
		for (MonitorAd monitorAd : monitor.getMonitorAds()) {
			if (monitorAd.getAd() == null || monitorAd.getAd().getClient() == null) {
				continue;
			}
			Client client = monitorAd.getAd().getClient();
			if (!partnerSlotAccessService.usesPartnerQuotaOnMonitor(client, monitor)) {
				continue;
			}
			int blocks =
					monitorAd.getBlockQuantity() != null
							? monitorAd.getBlockQuantity()
							: SharedConstants.MIN_QUANTITY_MONITOR_BLOCK;
			totalsByClient.merge(client.getId(), blocks, Integer::sum);
		}
		totalsByClient.forEach(
				(clientId, total) -> {
					if (total > SharedConstants.PARTNER_RESERVED_SLOTS) {
						throw new BusinessRuleException(MonitorValidationMessages.MONITOR_BLOCKS_BEYOND_LIMIT);
					}
				});
	}

	private void applyBlockQuantities(Map<UUID, MonitorAd> existingAfterRemoval, Map<UUID, MonitorAd> newMonitorAdsByAdId,
		Map<UUID, Integer> blockQuantities) {
		blockQuantities.forEach((adId, quantity) -> {
			MonitorAd monitorAd = existingAfterRemoval.getOrDefault(adId, newMonitorAdsByAdId.get(adId));
			if (monitorAd != null) {
				monitorAd.setBlockQuantity(quantity);
			}
		});
	}

	private List<UpdateBoxMonitorsAdRequestDto> buildRequestList(List<Ad> ads, Map<UUID, MonitorAd> existingAfterRemoval,
		Map<UUID, MonitorAd> newMonitorAdsByAdId) {
		List<UpdateBoxMonitorsAdRequestDto> requestList = ads.stream()
			.map(ad -> {
				MonitorAd monitorAd = existingAfterRemoval.getOrDefault(ad.getId(), newMonitorAdsByAdId.get(ad.getId()));
				if (monitorAd == null || monitorAd.getMonitor() == null || monitorAd.getMonitor().getBox() == null) {
					return null;
				}
				String link = adMediaLinkFactory.getLink(ad);
				return new UpdateBoxMonitorsAdRequestDto(ad, monitorAd, link);
			})
			.filter(Objects::nonNull)
			.toList();

		int totalBlockQuantity = requestList.stream().mapToInt(dto -> Optional.ofNullable(dto.getBlockQuantity()).orElse(0))
			.sum();

		if (totalBlockQuantity > SharedConstants.MAX_MONITOR_ADS) {
			throw new BusinessRuleException(MonitorValidationMessages.MONITOR_BLOCKS_BEYOND_LIMIT);
		}

		return requestList;
	}

	private void addNewMonitorAdsToMonitor(Monitor monitor, List<MonitorAd> newMonitorAds) {
		if (!newMonitorAds.isEmpty()) {
			monitor.getMonitorAds().addAll(newMonitorAds);
		}
	}

	private Map<UUID, MonitorAdRequestDto> mapAdsById(MonitorRequestDto request) {
		if (ValidateDataUtils.isNullOrEmpty(request.getAds())) {
			return Collections.emptyMap();
		}
		return request.getAds().stream()
			.collect(Collectors.toMap(MonitorAdRequestDto::getId, dto -> dto, (a, b) -> a));
	}
}
