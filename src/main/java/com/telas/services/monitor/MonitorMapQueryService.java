package com.telas.services.monitor;

import com.telas.dtos.response.MonitorMapsResponseDto;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.SubscriptionMonitor;
import com.telas.helpers.MonitorHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.MonitorRepository;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.MonitorBlocksUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitorMapQueryService {

	private static final double MAX_VIEWPORT_AXIS_SPAN_DEG = 0.5;
	private static final double VIEWPORT_AXIS_SPAN_FP_EPSILON = 1e-6;
	private static final int VIEWPORT_MAX_RESULTS = 300;

	private final AuthenticatedUserService authenticatedUserService;
	private final MonitorRepository repository;
	private final MonitorHelper helper;

	@Transactional(readOnly = true)
	public List<MonitorMapsResponseDto> findNearestActiveMonitors(String zipCode) {
		Client client = authenticatedUserService.getLoggedUser().client();
		return repository.findAvailableMonitorsByZipCode(zipCode, client.getId()).stream()
				.map(monitor -> toMonitorMapsResponseDto(monitor, client))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MonitorMapsResponseDto> findAvailableMonitorsInViewport(
			double minLat, double maxLat, double minLng, double maxLng) {
		validateViewportBounds(minLat, maxLat, minLng, maxLng);
		Client client = authenticatedUserService.getLoggedUser().client();
		Pageable pageable = PageRequest.of(0, VIEWPORT_MAX_RESULTS);
		return repository
				.findAvailableMonitorsInBounds(minLat, maxLat, minLng, maxLng, client.getId(), pageable)
				.getContent()
				.stream()
				.map(monitor -> toMonitorMapsResponseDto(monitor, client))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MonitorMapsResponseDto> findMonitorsForAdminMapByZipCode(String zipCode) {
		authenticatedUserService.validateAdmin();
		return repository.findAllForAdminMapByZipCode(zipCode).stream().map(this::toMonitorMapsResponseDto).toList();
	}

	void validateViewportBounds(double minLat, double maxLat, double minLng, double maxLng) {
		if (minLat >= maxLat || minLng >= maxLng) {
			throw new BusinessRuleException(MonitorValidationMessages.MONITOR_VIEWPORT_BOUNDS_INVALID);
		}
		if (minLat < -90 || maxLat > 90 || minLng < -180 || maxLng > 180) {
			throw new BusinessRuleException(MonitorValidationMessages.MONITOR_VIEWPORT_BOUNDS_INVALID);
		}
		double latSpan = maxLat - minLat;
		double lngSpan = maxLng - minLng;
		if (latSpan > MAX_VIEWPORT_AXIS_SPAN_DEG + VIEWPORT_AXIS_SPAN_FP_EPSILON
				|| lngSpan > MAX_VIEWPORT_AXIS_SPAN_DEG + VIEWPORT_AXIS_SPAN_FP_EPSILON) {
			throw new BusinessRuleException(MonitorValidationMessages.MONITOR_VIEWPORT_BOUNDS_INVALID);
		}
	}

	private MonitorMapsResponseDto toMonitorMapsResponseDto(Monitor monitor) {
		return toMonitorMapsResponseDto(monitor, null);
	}

	private MonitorMapsResponseDto toMonitorMapsResponseDto(Monitor monitor, Client viewingClient) {
		List<SubscriptionMonitor> subscriptionMonitors = helper.getSubscriptionsMonitorsFromMonitor(monitor.getId());

		int totalSubscriptionBlocks = MonitorBlocksUtils.sumSubscriptionBlocks(subscriptionMonitors);

		Map<UUID, SubscriptionMonitor> subsByClientId = subscriptionMonitors.stream()
			.filter(sm -> sm.getSubscription() != null && sm.getSubscription().getClient() != null)
			.collect(Collectors.toMap(sm -> sm.getSubscription().getClient().getId(), sm -> sm, (a, b) -> a));

		long unmatchedAdsCount = monitor.getMonitorAds().stream().filter(ma -> {
			UUID clientIdAd =
				ma.getAd() != null && ma.getAd().getClient() != null ? ma.getAd().getClient().getId() : null;
			return clientIdAd == null || !subsByClientId.containsKey(clientIdAd);
		}).count();

		int adsDailyMinutes = MonitorBlocksUtils.calculateAdsDailyDisplayTimeInMinutes(monitor.getMaxBlocks(),
			totalSubscriptionBlocks, unmatchedAdsCount);

		Client viewingPartner = viewingClient != null && viewingClient.isPartner() ? viewingClient : null;
		return new MonitorMapsResponseDto(monitor, adsDailyMinutes, viewingPartner);
	}
}
