package com.telas.services.impl;

import com.telas.dtos.request.CreatePartnerRequestDto;
import com.telas.dtos.request.PartnerAdRemovalRequestDto;
import com.telas.dtos.response.ClientMinResponseDto;
import com.telas.dtos.response.PendingAdAdminValidationResponseDto;
import com.telas.dtos.response.WishlistResponseDto;
import com.telas.entities.Ad;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.MonitorAd;
import com.telas.entities.VerificationCode;
import com.telas.enums.AdValidationType;
import com.telas.enums.CodeType;
import com.telas.enums.DefaultStatus;
import com.telas.enums.NotificationReference;
import com.telas.enums.Role;
import com.telas.helpers.AdPublicationNotificationHelper;
import com.telas.services.ad.AdUploadService;
import com.telas.helpers.ClientHelper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.AdRepository;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.MonitorAdRepository;
import com.telas.services.NotificationService;
import com.telas.services.PartnerPlatformSettingsService;
import com.telas.services.PartnerPortalService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.valitation.AdValidationMessages;
import com.telas.shared.constants.valitation.AuthValidationMessageConstants;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.services.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerPortalServiceImpl implements PartnerPortalService {

    private final ClientRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ClientHelper helper;
    private final VerificationCodeService verificationCodeService;
    private final AuthenticatedUserService authenticatedUserService;
    private final NotificationService notificationService;
    private final PartnerPlatformSettingsService partnerPlatformSettingsService;
    private final AdRepository adRepository;
    private final AdUploadService adUploadService;
    private final MonitorAdRepository monitorAdRepository;
    private final AdPublicationNotificationHelper adPublicationNotificationHelper;

    @Value("${front.base.url}")
    private String frontBaseUrl;

    @Override
    @Transactional
    public void changeRoleToPartner(UUID clientId) {
        Client admin = authenticatedUserService.validateAdmin().client();
        Client partner = repository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));

        if (!Role.PARTNER.equals(partner.getRole())) {
            CustomRevisionListener.setUsername(admin.getBusinessName());

            partner.setRole(Role.PARTNER);
            partner.setUsernameUpdate(admin.getBusinessName());
            repository.save(partner);
        }
    }

    @Override
    @Transactional
    public ClientMinResponseDto createPartnerByAdmin(CreatePartnerRequestDto request) {
        Client actor = authenticatedUserService.validateAdmin().client();
        validateCreatePartnerByAdminAccess(actor);
        helper.validateClientRequest(request, null);

        PasswordRequestDto passwordRequest = new PasswordRequestDto(request.getPassword(), request.getConfirmPassword());
        passwordRequest.validate();

        Client client = new Client(request);
        helper.verifyAddressesUnique(request.getAddresses(), client);

        client.setRole(Role.PARTNER);
        client.setStatus(DefaultStatus.ACTIVE);
        client.setPassword(passwordEncoder.encode(passwordRequest.getPassword()));

        VerificationCode verificationCode = verificationCodeService.savePreValidated(CodeType.PASSWORD, client);
        client.setVerificationCode(verificationCode);

        CustomRevisionListener.setUsername(actor.getBusinessName());
        client.setUsernameCreate(actor.getBusinessName());

        Client savedPartner = repository.save(client);
        notifyAdminsNewPartnerCreated(savedPartner);

        return new ClientMinResponseDto(savedPartner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingAdAdminValidationResponseDto> findMyPendingValidationAds() {
        Client partner = authenticatedUserService.validatePartner().client();
        return adRepository.findByClientIdAndValidation(partner.getId(), AdValidationType.PENDING).stream()
                .map(ad -> new PendingAdAdminValidationResponseDto(
                        ad,
                        adUploadService.getStringLinkFromAd(ad),
                        List.of()))
                .toList();
    }

    @Override
    @Transactional
    public void requestPartnerAdRemoval(UUID adId, PartnerAdRemovalRequestDto request) {
        Client partner = authenticatedUserService.validatePartner().client();
        Ad ad = adRepository.findByIdWithClientAndAdRequest(adId)
                .orElseThrow(() -> new ResourceNotFoundException(AdValidationMessages.AD_NOT_FOUND));
        if (ad.getClient() == null || !partner.getId().equals(ad.getClient().getId())) {
            throw new BusinessRuleException(AdValidationMessages.AD_NOT_OWNED_BY_PARTNER);
        }
        List<MonitorAd> placements = monitorAdRepository.findByAdIdWithMonitor(adId);
        boolean canRemove = ad.getOnAirNotifiedAt() != null
                || (placements != null && !placements.isEmpty());
        if (!canRemove) {
            throw new BusinessRuleException(AdValidationMessages.AD_REMOVAL_NOT_ALLOWED);
        }
        if (ad.getPartnerRemovalRequestedAt() != null) {
            throw new BusinessRuleException(AdValidationMessages.AD_REMOVAL_ALREADY_REQUESTED);
        }
        Monitor monitor = null;
        if (placements != null && !placements.isEmpty() && placements.get(0).getMonitor() != null) {
            monitor = placements.get(0).getMonitor();
        } else if (ad.getAdRequest() != null) {
            monitor = ad.getAdRequest().getTargetMonitor();
        }
        String message = request != null && request.getMessage() != null ? request.getMessage().trim() : "";
        ad.setPartnerRemovalRequestedAt(java.time.Instant.now());
        ad.setPartnerRemovalMessage(message.isEmpty() ? null : message);
        adRepository.save(ad);
        adPublicationNotificationHelper.notifyPartnerAdRemovalRequested(partner, ad, monitor, message);
    }

    @Override
    @Transactional
    public void addMonitorToWishlist(UUID monitorId) {
        Client client = authenticatedUserService.getLoggedUser().client();
        helper.addMonitorToWishlist(monitorId, client);
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistResponseDto getWishlistMonitors() {
        Client client = authenticatedUserService.getLoggedUser().client();

        if (client.getWishlist() == null) {
            throw new ResourceNotFoundException(ClientValidationMessages.WISHLIST_NOT_FOUND);
        }

        return new WishlistResponseDto(client.getWishlist());
    }

    private void notifyAdminsNewPartnerCreated(Client partner) {
        if (partner == null || !Role.PARTNER.equals(partner.getRole())) {
            return;
        }
        String contactEmail = "";
        if (partner.getContact() != null && partner.getContact().getEmail() != null) {
            contactEmail = partner.getContact().getEmail();
        }
        String adminLink = frontBaseUrl + "/admin/clients/" + partner.getId();
        Map<String, String> params = new HashMap<>();
        params.put("businessName", partner.getBusinessName() != null ? partner.getBusinessName() : "");
        params.put("contactEmail", contactEmail);
        params.put("clientId", partner.getId().toString());
        params.put("link", adminLink);
        repository.findAllAdmins().forEach(admin ->
                notificationService.save(NotificationReference.ADMIN_NEW_CLIENT_REGISTERED, admin, new HashMap<>(params), true));
    }

    private void validateCreatePartnerByAdminAccess(Client actor) {
        if (actor.isDeveloper()) {
            return;
        }
        if (actor.isAdmin() && partnerPlatformSettingsService.isAdminCanCreatePartnerEnabled()) {
            return;
        }
        throw new ForbiddenException(ClientValidationMessages.ADMIN_CREATE_PARTNER_DISABLED);
    }
}
