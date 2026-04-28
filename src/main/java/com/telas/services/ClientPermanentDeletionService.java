package com.telas.services;

import com.telas.dtos.response.PermanentDeletionRequirementsDto;
import com.telas.entities.Address;
import com.telas.entities.Ad;
import com.telas.entities.Attachment;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.entities.Subscription;
import com.telas.entities.SubscriptionMonitor;
import com.telas.enums.DefaultStatus;
import com.telas.enums.Role;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.AttachmentRepository;
import com.telas.repositories.ClientGrantedPermissionRepository;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.repositories.SubscriptionMonitorRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.impl.UnusedSingleAdDeletionService;
import com.telas.shared.constants.valitation.ClientValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientPermanentDeletionService {

    private final ClientRepository clientRepository;
    private final ClientGrantedPermissionRepository clientGrantedPermissionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMonitorRepository subscriptionMonitorRepository;
    private final MonitorRepository monitorRepository;
    private final AttachmentRepository attachmentRepository;
    private final BucketService bucketService;
    private final UnusedSingleAdDeletionService unusedSingleAdDeletionService;

    @Transactional(readOnly = true)
    public PermanentDeletionRequirementsDto getRequirements(UUID clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND);
        }
        List<Monitor> monitors = monitorRepository.findAllByAddressClientId(clientId);
        return new PermanentDeletionRequirementsDto(!monitors.isEmpty(), monitors.size());
    }

    @Transactional
    public void deleteClientAndOwnedData(UUID clientId, UUID monitorSuccessorClientId) {
        Client victim = clientRepository
                .findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));

        List<Monitor> partnerMonitors = monitorRepository.findAllByAddressClientId(clientId);
        if (!partnerMonitors.isEmpty()) {
            if (monitorSuccessorClientId == null) {
                throw new BusinessRuleException(ClientValidationMessages.PERMANENT_DELETE_REQUIRES_MONITOR_SUCCESSOR);
            }
            Client successor = resolveMonitorSuccessor(victim, monitorSuccessorClientId);
            transferPartnerScreensAndSubscriptions(victim, successor);
            clientRepository.save(successor);
        } else {
            List<Subscription> subs = new ArrayList<>(subscriptionRepository.findByClient_Id(clientId));
            subscriptionRepository.deleteAll(subs);
        }

        clientGrantedPermissionRepository.deleteByClient_Id(clientId);

        victim = clientRepository
                .findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));

        List<UUID> adIds = victim.getAds().stream().map(Ad::getId).toList();
        for (UUID adId : adIds) {
            unusedSingleAdDeletionService.deleteAdInNewTransaction(adId);
        }

        victim = clientRepository
                .findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));

        List<Attachment> attachments = new ArrayList<>(victim.getAttachments());
        for (Attachment att : attachments) {
            try {
                bucketService.deleteAttachment(AttachmentUtils.format(att));
            } catch (Exception ignored) {
            }
            attachmentRepository.delete(att);
        }

        victim = clientRepository
                .findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(ClientValidationMessages.USER_NOT_FOUND));
        clientRepository.delete(victim);
    }

    private Client resolveMonitorSuccessor(Client victim, UUID successorId) {
        if (successorId.equals(victim.getId())) {
            throw new BusinessRuleException(ClientValidationMessages.PERMANENT_DELETE_MONITOR_SUCCESSOR_INVALID);
        }
        Client successor = clientRepository
                .findById(successorId)
                .orElseThrow(() -> new BusinessRuleException(ClientValidationMessages.PERMANENT_DELETE_MONITOR_SUCCESSOR_INVALID));
        if (successor.isAdmin() || successor.isDeveloper() || !DefaultStatus.ACTIVE.equals(successor.getStatus())) {
            throw new BusinessRuleException(ClientValidationMessages.PERMANENT_DELETE_MONITOR_SUCCESSOR_INVALID);
        }
        return successor;
    }

    private void transferPartnerScreensAndSubscriptions(Client victim, Client successor) {
        successor.setRole(Role.PARTNER);

        List<Monitor> monitors = monitorRepository.findAllByAddressClientId(victim.getId());
        Set<Address> toReparent = new LinkedHashSet<>();
        for (Monitor m : monitors) {
            toReparent.add(m.getAddress());
        }
        for (Address addr : toReparent) {
            victim.getAddresses().remove(addr);
            addr.setClient(successor);
            if (!successor.getAddresses().contains(addr)) {
                successor.getAddresses().add(addr);
            }
        }

        Optional<Subscription> successorBonusOpt = subscriptionRepository.findActiveBonusSubscriptionByClientId(
                successor.getId());
        List<Subscription> victimSubs = new ArrayList<>(subscriptionRepository.findByClient_Id(victim.getId()));
        for (Subscription sub : victimSubs) {
            if (sub.isBonus() && successorBonusOpt.isPresent()
                    && !successorBonusOpt.get().getId().equals(sub.getId())) {
                Subscription succBonus = successorBonusOpt.get();
                List<SubscriptionMonitor> sms = new ArrayList<>(sub.getSubscriptionMonitors());
                for (SubscriptionMonitor sm : sms) {
                    Monitor m = sm.getMonitor();
                    int slots = sm.getSlotsQuantity();
                    sub.getSubscriptionMonitors().remove(sm);
                    subscriptionMonitorRepository.delete(sm);
                    boolean has = succBonus.getSubscriptionMonitors().stream()
                            .anyMatch(x -> x.getMonitor().getId().equals(m.getId()));
                    if (!has) {
                        SubscriptionMonitor newSm = new SubscriptionMonitor(succBonus, m, slots);
                        succBonus.getSubscriptionMonitors().add(newSm);
                    }
                }
                subscriptionRepository.delete(sub);
            } else {
                sub.setClient(successor);
                subscriptionRepository.save(sub);
            }
        }
        successorBonusOpt.ifPresent(subscriptionRepository::save);
    }
}
