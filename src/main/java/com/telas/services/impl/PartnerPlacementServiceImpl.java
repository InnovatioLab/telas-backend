package com.telas.services.impl;

import com.telas.entities.Cart;
import com.telas.entities.CartItem;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.enums.NotificationReference;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ForbiddenException;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.CartRepository;
import com.telas.services.PartnerPlacementService;
import com.telas.services.notification.AdminAdsNotificationService;
import com.telas.services.PartnerSlotAccessService;
import com.telas.services.partner.PartnerPlacementRules;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.AuthValidationMessageConstants;
import com.telas.shared.constants.valitation.CartValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.infra.security.services.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartnerPlacementServiceImpl implements PartnerPlacementService {

    private final AuthenticatedUserService authenticatedUserService;
    private final CartRepository cartRepository;
    private final ClientRepository clientRepository;
    private final AdminAdsNotificationService adminAdsNotificationService;
    private final PartnerSlotAccessService partnerSlotAccessService;
    private final PartnerPlacementRules partnerPlacementRules;

    @Value("${front.base.url}")
    private String frontBaseUrl;

    @Override
    @Transactional
    public void submitPlacementsFromActiveCart() {
        Client partner = authenticatedUserService.getLoggedUser().client();
        if (!partner.isPartner()) {
            throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
        }
        if (!partnerSlotAccessService.hasGlobalSlotsPermission(partner)) {
            throw new ForbiddenException(AuthValidationMessageConstants.ERROR_NO_PERMISSION);
        }

        Cart cart = cartRepository.findActiveByClientIdWithItens(partner.getId()).orElse(null);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessRuleException(CartValidationMessages.CART_EMPTY);
        }

        List<CartItem> foreignItems = cart.getItems().stream()
                .filter(item -> item.getMonitor() != null
                        && partnerPlacementRules.isForeignPlacementForPartner(partner, item.getMonitor()))
                .toList();

        if (foreignItems.isEmpty()) {
            throw new BusinessRuleException(CartValidationMessages.CART_EMPTY);
        }

        for (CartItem item : foreignItems) {
            Monitor monitor = item.getMonitor();
            int blocks = item.getBlockQuantity() != null
                    ? item.getBlockQuantity()
                    : SharedConstants.MIN_QUANTITY_MONITOR_BLOCK;
            if (!partnerSlotAccessService.canAddBlocks(partner, monitor, blocks)) {
                throw new BusinessRuleException(MonitorValidationMessages.MONITOR_BLOCKS_UNAVAILABLE);
            }
        }

        String monitorsSummary = foreignItems.stream()
                .map(item -> {
                    Monitor monitor = item.getMonitor();
                    String label = monitor.getAddress() != null
                            ? monitor.getAddress().resolveMapLocationName()
                            : monitor.getId().toString();
                    int blocks = item.getBlockQuantity() != null
                            ? item.getBlockQuantity()
                            : SharedConstants.MIN_QUANTITY_MONITOR_BLOCK;
                    return (label != null ? label : monitor.getId().toString()) + " (" + blocks + " blocks)";
                })
                .collect(Collectors.joining(", "));

        Map<String, String> params = Map.of(
                "partnerName", partner.getBusinessName() != null ? partner.getBusinessName() : "",
                "monitorsSummary", monitorsSummary,
                "link", frontBaseUrl + "/admin/clients/" + partner.getId()
        );

        adminAdsNotificationService.notifyAdmins(NotificationReference.ADMIN_PARTNER_PLACEMENT_REQUEST, params);

        cart.setActive(false);
        cartRepository.save(cart);
    }
}
