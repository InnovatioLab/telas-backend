package com.telas.services.impl;

import com.telas.dtos.request.HeartbeatRequestDto;
import com.telas.entities.Box;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.monitoring.entities.BoxHeartbeatEntity;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.services.BoxHeartbeatService;
import com.telas.services.HeartbeatRecoveryService;
import com.telas.services.HeartbeatRebootIncidentService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoxHeartbeatServiceImpl implements BoxHeartbeatService {

    private final BoxRepository boxRepository;
    private final BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;
    private final HeartbeatRebootIncidentService heartbeatRebootIncidentService;
    private final HeartbeatRecoveryService heartbeatRecoveryService;

    @Override
    @Transactional
    public void persistHeartbeat(HeartbeatRequestDto request) {
        if (ValidateDataUtils.isNullOrEmptyString(request.getBoxAddress())) {
            throw new IllegalArgumentException("boxAddress is required");
        }
        Box box = boxRepository.findByAddress(request.getBoxAddress().trim())
                .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
        Instant now = Instant.now();
        Optional<BoxHeartbeatEntity> prior = boxHeartbeatEntityRepository.findByBox_Id(box.getId());
        if (prior.isPresent()) {
            BoxHeartbeatEntity heartbeatRow = prior.get();
            heartbeatRebootIncidentService.recordIfHostRebootDetected(
                    box, heartbeatRow.getMetadataJson(), request.getMetadata());
            heartbeatRow.setLastSeenAt(now);
            heartbeatRow.setReportedVersion(request.getReportedVersion());
            heartbeatRow.setMetadataJson(request.getMetadata());
            heartbeatRow.setUpdatedAt(now);
            boxHeartbeatEntityRepository.save(heartbeatRow);
            heartbeatRecoveryService.recoverAfterSuccessfulHeartbeat(box);
            return;
        }
        BoxHeartbeatEntity entity = new BoxHeartbeatEntity();
        entity.setBox(box);
        entity.setLastSeenAt(now);
        entity.setReportedVersion(request.getReportedVersion());
        entity.setMetadataJson(request.getMetadata());
        entity.setUpdatedAt(now);
        boxHeartbeatEntityRepository.save(entity);
        heartbeatRecoveryService.recoverAfterSuccessfulHeartbeat(box);
    }
}
