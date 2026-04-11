package com.telas.services;

import com.telas.dtos.request.AcknowledgeIncidentRequestDto;
import com.telas.dtos.response.IncidentResponseDto;
import com.telas.infra.security.model.AuthenticatedUser;

import java.util.UUID;

public interface IncidentCommandService {

    IncidentResponseDto acknowledge(UUID incidentId, AcknowledgeIncidentRequestDto dto, AuthenticatedUser user);

    IncidentResponseDto resolve(UUID incidentId);
}
