package com.telas.services.monitor;

import com.telas.dtos.request.AttachmentRequestDto;
import com.telas.dtos.request.PartnerAdRequestToAdminDto;
import com.telas.dtos.request.PartnerAdSubmissionRequestDto;
import com.telas.dtos.request.PartnerDirectAdRequestDto;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.dtos.response.MonitorResponseDto;
import com.telas.entities.*;
import com.telas.enums.AdValidationType;
import com.telas.enums.NotificationReference;
import com.telas.enums.PartnerSubmissionMode;
import com.telas.helpers.ClientHelper;
import com.telas.helpers.MonitorHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdRepository;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.MonitorAdRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.services.BucketService;
import com.telas.services.NotificationService;
import com.telas.services.PartnerSlotAccessService;
import com.telas.services.ad.AdValidationService;
import com.telas.services.notification.AdminAdsNotificationService;
import com.telas.services.partner.PartnerPlacementRules;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AuthValidationMessageConstants;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PartnerMonitorAdService {

	private final AuthenticatedUserService authenticatedUserService;
	private final MonitorRepository monitorRepository;
	private final AdRepository adRepository;
	private final MonitorAdRepository monitorAdRepository;
	private final ClientRepository clientRepository;
	private final BucketService bucketService;
	private final MonitorHelper helper;
	private final PartnerSlotAccessService partnerSlotAccessService;
	private final PartnerPlacementRules partnerPlacementRules;
	private final AdValidationService adValidationService;
	private final NotificationService notificationService;
	private final AdminAdsNotificationService adminAdsNotificationService;
	private final ClientHelper clientHelper;
	private final MonitorCrudService monitorCrudService;

	@Value("${front.base.url}")
	private String frontBaseUrl;

	@Transactional(readOnly = true)
	public List<MonitorResponseDto> findMonitorsForLoggedPartner() {
		Client partner = authenticatedUserService.validatePartner().client();
		LinkedHashSet<UUID> monitorIds = new LinkedHashSet<>();
		monitorRepository.findAllByAddressClientId(partner.getId()).forEach(m -> monitorIds.add(m.getId()));
		monitorAdRepository.findDistinctMonitorIdsByAdvertiserClientIdApproved(partner.getId())
				.forEach(monitorIds::add);
		adRepository.findDistinctPendingTargetMonitorIdsForPartner(partner.getId()).forEach(monitorIds::add);

		return monitorIds.stream()
				.map(monitorCrudService::findEntityById)
				.map(monitor -> {
					List<MonitorAdResponseDto> myAds =
							helper.getPartnerAdvertiserAdsForPortal(monitor, partner.getId());
					boolean partnerOwned = partnerPlacementRules.partnerOwnsMonitorAddress(partner, monitor);
					if (myAds.isEmpty() && !partnerOwned) {
						return null;
					}
					return new MonitorResponseDto(
							monitor,
							myAds,
							adRepository.countAllApprovedNotInMonitor(monitor.getId()),
							myAds.size());
				})
				.filter(Objects::nonNull)
				.toList();
	}

	@Transactional(readOnly = true)
	public MonitorResponseDto findMonitorForPartnerPlacement(UUID monitorId) {
		Client partner = authenticatedUserService.validatePartner().client();
		Monitor monitor = monitorCrudService.findEntityById(monitorId);
		return new MonitorResponseDto(
				monitor,
				helper.getMonitorAdsResponse(monitor),
				adRepository.countAllApprovedNotInMonitor(monitor.getId()));
	}

	@Transactional
	public UUID uploadDirectAdToMonitor(UUID monitorId, AttachmentRequestDto request) {
		request.validate();
		authenticatedUserService.validateNonPartner();
		Client actor = authenticatedUserService.validateAdmin().client();
		return executeDirectAdUpload(monitorId, request, null, actor, false, false);
	}

	@Transactional
	public UUID uploadPartnerDirectAdToMonitor(UUID monitorId, PartnerDirectAdRequestDto request) {
		Client actor = authenticatedUserService.validatePartner().client();
		Monitor monitor = monitorCrudService.findEntityById(monitorId);
		boolean partnerForeignPlacement = !partnerPlacementRules.partnerOwnsMonitorAddress(actor, monitor);
		if (partnerForeignPlacement) {
			request.validateForeignPlacement();
			if (!partnerSlotAccessService.hasGlobalSlotsPermission(actor)) {
				throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
			}
		} else {
			request.validate();
		}
		PartnerAdSubmissionRequestDto submission = new PartnerAdSubmissionRequestDto();
		submission.setSubmissionMode(PartnerSubmissionMode.READY_CREATIVE);
		submission.setAttachment(request.getAttachment());
		submission.setOptionalLabel(request.getAdLabel());
		return submitPartnerAdSubmission(monitorId, submission);
	}

	@Transactional
	public UUID submitPartnerAdSubmission(UUID monitorId, PartnerAdSubmissionRequestDto request) {
		request.validate();
		Client actor = authenticatedUserService.validatePartner().client();
		Monitor monitor = monitorCrudService.findEntityById(monitorId);
		boolean partnerForeignPlacement = !partnerPlacementRules.partnerOwnsMonitorAddress(actor, monitor);
		if (partnerForeignPlacement) {
			if (!partnerSlotAccessService.hasGlobalSlotsPermission(actor)) {
				throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
			}
		} else {
			if (request.getSubmissionMode() == PartnerSubmissionMode.ADMIN_MATERIALS
					|| request.getSubmissionMode() == PartnerSubmissionMode.PARTNER_FINISHED_CREATIVE) {
				throw new BusinessRuleException(MonitorValidationMessages.PARTNER_MATERIALS_FOREIGN_ONLY);
			}
		}

		return switch (request.getSubmissionMode()) {
			case READY_CREATIVE -> executeDirectAdUpload(
					monitorId,
					request.getAttachment(),
					resolveOptionalLabel(request),
					actor,
					partnerForeignPlacement,
					partnerForeignPlacement);
			case ADMIN_MATERIALS -> submitPartnerMaterialsRequest(monitorId, request, actor, monitor);
			case PARTNER_FINISHED_CREATIVE -> submitPartnerFinishedCreativeRequest(monitorId, request, actor, monitor);
		};
	}

	private String resolveOptionalLabel(PartnerAdSubmissionRequestDto request) {
		if (request.getOptionalLabel() != null && !request.getOptionalLabel().isBlank()) {
			return request.getOptionalLabel().trim();
		}
		if (request.getAttachment() != null && request.getAttachment().getName() != null) {
			return request.getAttachment().getName().replaceAll("\\.[^.]+$", "");
		}
		return null;
	}

	private UUID submitPartnerMaterialsRequest(
			UUID monitorId,
			PartnerAdSubmissionRequestDto request,
			Client partner,
			Monitor monitor) {
		PartnerAdRequestToAdminDto materialsRequest = new PartnerAdRequestToAdminDto();
		materialsRequest.setTargetMonitorId(monitorId);
		materialsRequest.setAttachmentIds(request.getAttachmentIds());
		materialsRequest.setOptionalLabel(request.getOptionalLabel());

		AdRequest created = clientHelper.createPartnerAdRequest(materialsRequest, partner, monitor);
		notifyAdminsCreateAdSubmitted(partner, monitor, created);
		notifyPartnerSubmissionAck(partner, monitor, "Create Ad");
		return created.getId();
	}

	private UUID submitPartnerFinishedCreativeRequest(
			UUID monitorId,
			PartnerAdSubmissionRequestDto request,
			Client partner,
			Monitor monitor) {
		List<AttachmentRequestDto> batch = (request.getAttachments() != null && !request.getAttachments().isEmpty())
				? request.getAttachments()
				: List.of(request.getAttachment());

		AdRequest first = null;
		for (AttachmentRequestDto attachment : batch) {
			PartnerAdRequestToAdminDto dto = new PartnerAdRequestToAdminDto();
			dto.setTargetMonitorId(monitorId);
			dto.setOptionalLabel(request.getOptionalLabel());
			AdRequest created = clientHelper.createPartnerFinishedCreativeRequest(dto, partner, monitor, attachment);
			if (first == null) {
				first = created;
			}
		}
		notifyAdminsFinishedAdSubmitted(partner, monitor, first);
		notifyPartnerSubmissionAck(partner, monitor, "Finished Ad");
		return first.getId();
	}

	private UUID executeDirectAdUpload(
			UUID monitorId,
			AttachmentRequestDto request,
			String adLabel,
			Client actor,
			boolean partnerForeignPlacement,
			boolean deferBoxSyncForForeign) {
		Monitor monitor = monitorCrudService.findEntityById(monitorId);

		final Client adOwner;
		if (actor.isPartner()) {
			validatePartnerPlacementAccess(actor, monitor);
			adOwner = clientRepository.findById(actor.getId())
					.orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
			partnerSlotAccessService.validatePartnerAdCreationAllowed(adOwner, monitor, true);
		} else {
			adOwner = actor;
		}

		Ad ad = new Ad(request, adOwner);
		if (adLabel != null && !adLabel.isBlank()) {
			ad.setName(adLabel.trim());
		}
		ad.setUsernameCreate(actor.getBusinessName());
		Ad saved = adRepository.save(ad);
		adOwner.getAds().add(saved);
		clientRepository.save(adOwner);

		bucketService.upload(
			request.getBytes(),
			AttachmentUtils.format(saved),
			request.getType(),
			new java.io.ByteArrayInputStream(request.getBytes())
		);

		List<Ad> nextAds = new ArrayList<>(monitor.getAds());
		nextAds.add(saved);
		monitorCrudService.validateMonitorAdsQuotas(monitor, nextAds);

		monitor.getMonitorAds().add(new MonitorAd(monitor, saved));
		monitorRepository.save(monitor);
		adValidationService.validateAd(saved, actor, AdValidationType.APPROVED, null);

		if (partnerForeignPlacement && deferBoxSyncForForeign) {
			notifyAdminsPartnerForeignAdReadyForDispatch(actor, monitor, saved);
		}

		return saved.getId();
	}

	private void validatePartnerPlacementAccess(Client partner, Monitor monitor) {
		if (!partnerSlotAccessService.canAddBlocks(
				partner, monitor, SharedConstants.MIN_QUANTITY_MONITOR_BLOCK)) {
			throw new BusinessRuleException(MonitorValidationMessages.MONITOR_BLOCKS_UNAVAILABLE);
		}
	}

	private void notifyAdminsPartnerForeignAdReadyForDispatch(Client partner, Monitor monitor, Ad ad) {
		String monitorLabel = monitor.getAddress() != null
				? monitor.getAddress().resolveMapLocationName()
				: monitor.getId().toString();
		Map<String, String> params = new HashMap<>();
		params.put("partnerName", partner.getBusinessName() != null ? partner.getBusinessName() : "");
		params.put("monitorLabel", monitorLabel != null ? monitorLabel : "");
		params.put("adLabel", ad.getName() != null ? ad.getName() : "");
		params.put("adId", ad.getId().toString());
		params.put("clientId", partner.getId().toString());
		params.put("link", frontBaseUrl + "/admin/ads");
		adminAdsNotificationService.notifyAdmins(
				NotificationReference.ADMIN_PARTNER_FOREIGN_AD_SUBMITTED, params);
	}

	private void notifyAdminsCreateAdSubmitted(Client partner, Monitor monitor, AdRequest adRequest) {
		adminAdsNotificationService.notifyAdmins(
				NotificationReference.ADMIN_PARTNER_PLACEMENT_REQUEST,
				buildPartnerAdRequestAdminParams(partner, monitor, adRequest));
	}

	private void notifyAdminsFinishedAdSubmitted(Client partner, Monitor monitor, AdRequest adRequest) {
		adminAdsNotificationService.notifyAdmins(
				NotificationReference.ADMIN_PARTNER_FINISHED_AD_SUBMITTED,
				buildPartnerAdRequestAdminParams(partner, monitor, adRequest));
	}

	private Map<String, String> buildPartnerAdRequestAdminParams(
			Client partner, Monitor monitor, AdRequest adRequest) {
		String monitorLabel = resolveMonitorLabel(monitor);
		Map<String, String> params = new HashMap<>();
		params.put("partnerName", partner.getBusinessName() != null ? partner.getBusinessName() : "");
		params.put("monitorLabel", monitorLabel);
		params.put("instructions", adRequest.getSlogan() != null ? adRequest.getSlogan() : "");
		params.put("adId", adRequest.getId().toString());
		params.put("clientId", partner.getId().toString());
		params.put("link", frontBaseUrl + "/admin/ad-requests");
		return params;
	}

	private void notifyPartnerSubmissionAck(Client partner, Monitor monitor, String submissionType) {
		boolean createAd = "Create Ad".equals(submissionType);
		Map<String, String> params = new HashMap<>();
		params.put("name", partner.getBusinessName() != null ? partner.getBusinessName() : "");
		params.put("submissionType", submissionType);
		params.put("submissionKind", createAd ? "create_ad" : "finished_ad");
		params.put("monitorLabel", resolveMonitorLabel(monitor));
		params.put("link", frontBaseUrl + (createAd ? "/partner/screens?tab=requests" : "/partner/ads-review"));
		params.put("linkLabel", createAd ? "My screens" : "Review ads");
		notificationService.save(NotificationReference.CLIENT_PARTNER_SUBMISSION_ACK, partner, params, true);
	}

	private String resolveMonitorLabel(Monitor monitor) {
		if (monitor.getAddress() != null) {
			String label = monitor.getAddress().resolveMapLocationName();
			if (label != null) {
				return label;
			}
		}
		return monitor.getId().toString();
	}
}
