package com.telas.services.impl;

import com.telas.dtos.request.AcknowledgeIncidentRequestDto;
import com.telas.dtos.response.IncidentResponseDto;
import com.telas.entities.Client;
import com.telas.entities.Contact;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.model.AuthenticatedUser;
import com.telas.monitoring.entities.IncidentEntity;
import com.telas.monitoring.repositories.IncidentEntityRepository;
import com.telas.repositories.ClientRepository;
import com.telas.shared.constants.valitation.MonitoringValidationMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentCommandServiceImplTest {

    @Mock
    private IncidentEntityRepository incidentEntityRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private IncidentCommandServiceImpl service;

    private final UUID incidentId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    private Client actingClient;
    private AuthenticatedUser user;

    @BeforeEach
    void setUp() {
        actingClient = new Client();
        actingClient.setId(clientId);
        Contact contact = new Contact();
        contact.setEmail("admin@example.com");
        actingClient.setContact(contact);
        user = new AuthenticatedUser(actingClient);
    }

    @Test
    void acknowledge_throwsWhenIncidentMissing() {
        when(incidentEntityRepository.findById(incidentId)).thenReturn(Optional.empty());

        AcknowledgeIncidentRequestDto dto = new AcknowledgeIncidentRequestDto();
        dto.setReason("Investigating");

        assertThatThrownBy(() -> service.acknowledge(incidentId, dto, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MonitoringValidationMessages.INCIDENT_NOT_FOUND);
    }

    @Test
    void acknowledge_throwsWhenAlreadyResolved() {
        IncidentEntity incident = openIncident();
        incident.setClosedAt(Instant.now());
        when(incidentEntityRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        AcknowledgeIncidentRequestDto dto = new AcknowledgeIncidentRequestDto();
        dto.setReason("Too late");

        assertThatThrownBy(() -> service.acknowledge(incidentId, dto, user))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(MonitoringValidationMessages.INCIDENT_ALREADY_RESOLVED_CANNOT_ACKNOWLEDGE);
    }

    @Test
    void acknowledge_idempotentWhenAlreadyAcknowledged() {
        IncidentEntity incident = openIncident();
        incident.setAcknowledgedAt(Instant.now());
        incident.setAcknowledgeReason("Earlier");
        when(incidentEntityRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        AcknowledgeIncidentRequestDto dto = new AcknowledgeIncidentRequestDto();
        dto.setReason("Again");

        IncidentResponseDto result = service.acknowledge(incidentId, dto, user);

        assertThat(result.getAcknowledgeReason()).isEqualTo("Earlier");
        verify(incidentEntityRepository, never()).save(any());
    }

    @Test
    void acknowledge_persistsReasonAndActor() {
        IncidentEntity incident = openIncident();
        when(incidentEntityRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(clientRepository.getReferenceById(clientId)).thenReturn(actingClient);
        when(incidentEntityRepository.save(any(IncidentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AcknowledgeIncidentRequestDto dto = new AcknowledgeIncidentRequestDto();
        dto.setReason("On call");

        IncidentResponseDto result = service.acknowledge(incidentId, dto, user);

        assertThat(result.getAcknowledgeReason()).isEqualTo("On call");
        assertThat(result.getAcknowledgedById()).isEqualTo(clientId);
        verify(incidentEntityRepository).save(incident);
    }

    @Test
    void resolve_throwsWhenIncidentMissing() {
        when(incidentEntityRepository.findById(incidentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(incidentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MonitoringValidationMessages.INCIDENT_NOT_FOUND);
    }

    @Test
    void resolve_idempotentWhenAlreadyClosed() {
        Instant closed = Instant.parse("2024-01-01T12:00:00Z");
        IncidentEntity incident = openIncident();
        incident.setClosedAt(closed);
        when(incidentEntityRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        IncidentResponseDto result = service.resolve(incidentId);

        assertThat(result.getClosedAt()).isEqualTo(closed);
        verify(incidentEntityRepository, never()).save(any());
    }

    @Test
    void resolve_setsClosedAtWhenOpen() {
        IncidentEntity incident = openIncident();
        when(incidentEntityRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentEntityRepository.save(any(IncidentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        IncidentResponseDto result = service.resolve(incidentId);

        assertThat(result.getClosedAt()).isNotNull();
        verify(incidentEntityRepository).save(incident);
    }

    private IncidentEntity openIncident() {
        IncidentEntity incident = new IncidentEntity();
        incident.setId(incidentId);
        incident.setIncidentType("HEARTBEAT_STALE");
        incident.setSeverity("WARNING");
        incident.setOpenedAt(Instant.now());
        return incident;
    }
}
