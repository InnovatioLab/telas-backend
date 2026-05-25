package com.telas.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.MonitorRequestDto;
import com.telas.dtos.request.PartnerAdSubmissionRequestDto;
import com.telas.dtos.request.PartnerDirectAdRequestDto;
import com.telas.dtos.request.filters.FilterMonitorRequestDto;
import com.telas.dtos.response.*;
import com.telas.entities.Monitor;
import com.telas.entities.Subscription;
import com.telas.services.MonitorService;
import com.telas.services.RemoveMonitorAdsOutcome;
import com.telas.services.monitor.MonitorCrudService;
import com.telas.services.monitor.MonitorMapQueryService;
import com.telas.services.monitor.PartnerMonitorAdService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

	private final MonitorCrudService monitorCrudService;
	private final MonitorMapQueryService monitorMapQueryService;
	private final PartnerMonitorAdService partnerMonitorAdService;

	@Override
	@Transactional
	public UUID save(MonitorRequestDto requestDto, UUID monitorId) throws JsonProcessingException {
		return monitorCrudService.save(requestDto, monitorId);
	}

	@Override
	@Transactional
	public RemoveMonitorAdsOutcome removeMonitorAdsFromSubscription(Subscription subscription) {
		return monitorCrudService.removeMonitorAdsFromSubscription(subscription);
	}

	@Override
	@Transactional(readOnly = true)
	public MonitorResponseDto findById(UUID monitorId) {
		return monitorCrudService.findById(monitorId);
	}

	@Override
	@Transactional
	public Monitor findEntityById(UUID monitorId) {
		return monitorCrudService.findEntityById(monitorId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MonitorMapsResponseDto> findNearestActiveMonitors(String zipCode) {
		return monitorMapQueryService.findNearestActiveMonitors(zipCode);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MonitorMapsResponseDto> findAvailableMonitorsInViewport(
			double minLat, double maxLat, double minLng, double maxLng) {
		return monitorMapQueryService.findAvailableMonitorsInViewport(minLat, maxLat, minLng, maxLng);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MonitorMapsResponseDto> findMonitorsForAdminMapByZipCode(String zipCode) {
		return monitorMapQueryService.findMonitorsForAdminMapByZipCode(zipCode);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MonitorsBoxMinResponseDto> findAllMonitors() {
		return monitorCrudService.findAllMonitors();
	}

	@Override
	@Transactional(readOnly = true)
	public List<MonitorValidAdResponseDto> findValidAdsForMonitor(UUID monitorId, String name) {
		return monitorCrudService.findValidAdsForMonitor(monitorId, name);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginationResponseDto<List<MonitorResponseDto>> findAllByFilters(FilterMonitorRequestDto request) {
		return monitorCrudService.findAllByFilters(request);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MonitorResponseDto> findMonitorsForLoggedPartner() {
		return partnerMonitorAdService.findMonitorsForLoggedPartner();
	}

	@Override
	@Transactional(readOnly = true)
	public MonitorResponseDto findMonitorForPartnerPlacement(UUID monitorId) {
		return partnerMonitorAdService.findMonitorForPartnerPlacement(monitorId);
	}

	@Override
	@Transactional
	public UUID uploadDirectAdToMonitor(UUID monitorId, AttachmentRequestDto request) {
		return partnerMonitorAdService.uploadDirectAdToMonitor(monitorId, request);
	}

	@Override
	@Transactional
	public UUID uploadPartnerDirectAdToMonitor(UUID monitorId, PartnerDirectAdRequestDto request) {
		return partnerMonitorAdService.uploadPartnerDirectAdToMonitor(monitorId, request);
	}

	@Override
	@Transactional
	public UUID submitPartnerAdSubmission(UUID monitorId, PartnerAdSubmissionRequestDto request) {
		return partnerMonitorAdService.submitPartnerAdSubmission(monitorId, request);
	}

	@Override
	@Transactional
	public void deleteAvailableAd(UUID monitorId, UUID adId) {
		monitorCrudService.deleteAvailableAd(monitorId, adId);
	}

	@Override
	@Transactional
	public void delete(UUID monitorId) {
		monitorCrudService.delete(monitorId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MonitorValidAdResponseDto> findCurrentDisplayedAdsFromBox(UUID monitorId) {
		return monitorCrudService.findCurrentDisplayedAdsFromBox(monitorId);
	}
}
