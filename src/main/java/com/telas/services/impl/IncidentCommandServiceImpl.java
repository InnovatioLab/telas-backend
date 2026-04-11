package com.telas.services.impl;

import com.telas.dtos.request.AcknowledgeIncidentRequestDto;
import com.telas.dtos.response.IncidentResponseDto;
import com.telas.entities.Client;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.monitoring.entities.IncidentEntity;
import com.telas.monitoring.repositories.IncidentEntityRepository;
import com.telas.repositories.ClientRepository;
import com.telas.services.IncidentCommandService;
import com.telas.shared.constants.valitation.MonitoringValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentCommandServiceImpl implements IncidentCommandService {

    private final IncidentEntityRepository incidentEntityRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public IncidentResponseDto acknowledge(
            UUID incidentId, AcknowledgeIncidentRequestDto dto, AuthenticatedUser user) {
        IncidentEntity incident =
                incidentEntityRepository
                        .findById(incidentId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MonitoringValidationMessages.INCIDENT_NOT_FOUND));
        if (incident.getClosedAt() != null) {
            throw new BusinessRuleException(
                    MonitoringValidationMessages.INCIDENT_ALREADY_RESOLVED_CANNOT_ACKNOWLEDGE);
        }
        if (incident.getAcknowledgedAt() != null) {
            return new IncidentResponseDto(incident);
        }
        UUID clientId = user.client().getId();
        Client acknowledgedBy = clientRepository.getReferenceById(clientId);
        incident.setAcknowledgedAt(Instant.now());
        incident.setAcknowledgeReason(dto.getReason());
        incident.setAcknowledgedBy(acknowledgedBy);
        incidentEntityRepository.save(incident);
        return new IncidentResponseDto(incident);
    }

    @Override
    @Transactional
    public IncidentResponseDto resolve(UUID incidentId) {
        IncidentEntity incident =
                incidentEntityRepository
                        .findById(incidentId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MonitoringValidationMessages.INCIDENT_NOT_FOUND));
        if (incident.getClosedAt() != null) {
            return new IncidentResponseDto(incident);
        }
        incident.setClosedAt(Instant.now());
        incidentEntityRepository.save(incident);
        return new IncidentResponseDto(incident);
    }
}
