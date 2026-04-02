package com.telas.services.impl;

import com.telas.dtos.request.BoxRequestDto;
import com.telas.dtos.request.StatusBoxMonitorsRequestDto;
import com.telas.dtos.response.BoxMonitorAdResponseDto;
import com.telas.dtos.response.BoxResponseDto;
import com.telas.entities.Box;
import com.telas.entities.BoxAddress;
import com.telas.entities.Monitor;
import com.telas.enums.DefaultStatus;
import com.telas.enums.NotificationReference;
import com.telas.helpers.BoxHelper;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.BoxRepository;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.BoxService;
import com.telas.services.NotificationService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.DateUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoxServiceImpl implements BoxService {

	private final BoxRepository repository;

	private final AuthenticatedUserService authenticatedUserService;

	private final NotificationService notificationService;

	private final MonitorRepository monitorRepository;

	private final ClientRepository clientRepository;

	private final BoxHelper helper;


	@Override
	@Transactional(readOnly = true)
	public List<BoxResponseDto> findAll() {
		authenticatedUserService.validateAdmin();

		return repository.findAll().stream().map(BoxResponseDto::new).toList();
	}


	@Override
	@Transactional
	public void save(BoxRequestDto request, UUID boxId) {
		authenticatedUserService.validateAdmin();

		BoxAddress boxAddress = helper.getBoxAddress(request.getBoxAddressId());
		Monitor monitor = findMonitorById(request.getMonitorId());

		Box box = (boxId != null) ? updateBox(request, boxId, boxAddress, monitor) : new Box(boxAddress, monitor);

		repository.save(box);
	}


	@Override
	@Transactional
	public List<BoxMonitorAdResponseDto> getMonitorsAdsByAddress(String address) {
		Box box = findByAddress(address);

		if (activateBoxIfInactive(box)) {
			repository.save(box);
		}

		return helper.getBoxMonitorAdResponse(box);
	}


	@Override
	@Transactional
	public void updateHealth(StatusBoxMonitorsRequestDto request) {
		HealthUpdateContext ctx = HealthUpdateContext.from(request);

		if (!ValidateDataUtils.isNullOrEmptyString(request.getIp())) {
			updateHealthByBoxIp(request.getIp(), ctx);
			return;
		}

		if (Objects.nonNull(request.getMonitorId())) {
			updateHealthByMonitorId(request.getMonitorId(), ctx);
		}
	}


	private void updateHealthByBoxIp(String ip, HealthUpdateContext ctx) {
		Box box = findByAddress(ip);
		box.setActive(ctx.isActive());
		repository.save(box);

		if (box.getMonitors().isEmpty()) {
			return;
		}

		syncMonitorsActiveState(box.getMonitors(), ctx.isActive());
		Map<String, String> params = buildBoxStatusNotificationParams(ip, ctx, formatLinkedMonitorAddresses(box));
		notifyAdmins(NotificationReference.BOX_STATUS_UPDATED, params);
	}


	private void updateHealthByMonitorId(UUID monitorId, HealthUpdateContext ctx) {
		Monitor monitor = findMonitorById(monitorId);
		monitor.setActive(ctx.isActive());
		monitorRepository.save(monitor);
		notifyAdmins(NotificationReference.MONITOR_STATUS_UPDATED, buildMonitorStatusNotificationParams(monitor, ctx));
	}


	private void syncMonitorsActiveState(List<Monitor> monitors, boolean isActive) {
		monitors.forEach(m -> m.setActive(isActive));
		monitorRepository.saveAll(monitors);
	}


	private String formatLinkedMonitorAddresses(Box box) {
		return box.getMonitors().stream().map(m -> m.getAddress().getCoordinatesParams()).collect(Collectors.joining("; "));
	}


	private Map<String, String> buildBoxStatusNotificationParams(String ip, HealthUpdateContext ctx, String monitorAddresses) {
		Map<String, String> params = new HashMap<>();
		params.put("ip", ip);
		params.put("statusLabel", ctx.statusLabel());
		params.put("monitorAddresses", monitorAddresses);
		params.put("notifiedAt", ctx.notifiedAt());
		return params;
	}


	private Map<String, String> buildMonitorStatusNotificationParams(Monitor monitor, HealthUpdateContext ctx) {
		Map<String, String> params = new HashMap<>();
		params.put("monitorAddress", resolveMonitorCoordinates(monitor));
		params.put("statusLabel", ctx.statusLabel());
		params.put("notifiedAt", ctx.notifiedAt());
		return params;
	}


	private String resolveMonitorCoordinates(Monitor monitor) {
		return monitor.getAddress() != null ? monitor.getAddress().getCoordinatesParams() : "Unknown";
	}


	private void notifyAdmins(NotificationReference reference, Map<String, String> params) {
		clientRepository.findAllAdmins().forEach(admin -> notificationService.save(reference, admin, params, false));
	}


	private record HealthUpdateContext(boolean isActive, String statusLabel, String notifiedAt) {

		static HealthUpdateContext from(StatusBoxMonitorsRequestDto request) {
			boolean isActive = DefaultStatus.ACTIVE.equals(request.getStatus());
			String statusLabel = isActive ? "reactivated" : "deactivated";
			String notifiedAt = DateUtils.formatInstantToUsDateTime(Instant.now());
			return new HealthUpdateContext(isActive, statusLabel, notifiedAt);
		}
	}


	private Monitor findMonitorById(UUID monitorId) {
		return monitorRepository.findById(monitorId)
			.orElseThrow(() -> new ResourceNotFoundException(MonitorValidationMessages.MONITOR_NOT_FOUND));
	}


	private boolean activateBoxIfInactive(Box box) {
		if (!box.isActive()) {
			box.setActive(true);
			return true;
		}
		return false;
	}


	private Box findById(UUID boxId) {
		return repository.findById(boxId)
			.orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
	}


	private Box findByAddress(String address) {
		return repository.findByAddress(address)
			.orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
	}


	private Box updateBox(BoxRequestDto request, UUID boxId, BoxAddress boxAddress, Monitor monitor) {
		Box box = findById(boxId);

		if (!box.getBoxAddress().getId().equals(boxAddress.getId())) {
			box.setBoxAddress(boxAddress);
		}

		box.setActive(request.isActive());

		if (monitor.getBox() != null && !monitor.getBox().getId().equals(box.getId())) {
			Box previousBox = monitor.getBox();
			previousBox.getMonitors().removeIf(m -> m.getId().equals(monitor.getId()));
		}

		box.getMonitors().forEach(m -> {
			m.setBox(null);
			monitorRepository.save(m);
		});

		box.getMonitors().clear();

		monitor.setBox(box);
		monitorRepository.save(monitor);

		box.getMonitors().add(monitor);

		return box;

	}
}
