package com.telas.services.impl;

import com.telas.dtos.request.HeartbeatRequestDto;
import com.telas.entities.Box;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.monitoring.entities.BoxHeartbeatEntity;
import com.telas.monitoring.repositories.BoxHeartbeatEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.services.BoxHeartbeatService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BoxHeartbeatServiceImpl implements BoxHeartbeatService {

    private final BoxRepository boxRepository;
    private final BoxHeartbeatEntityRepository boxHeartbeatEntityRepository;

    @Override
    @Transactional
    public void record(HeartbeatRequestDto request) {
        if (ValidateDataUtils.isNullOrEmptyString(request.getBoxAddress())) {
            throw new IllegalArgumentException("boxAddress is required");
        }
        Box box = boxRepository.findByAddress(request.getBoxAddress().trim())
                .orElseThrow(() -> new ResourceNotFoundException(BoxValidationMessages.BOX_NOT_FOUND));
        Instant now = Instant.now();
        boxHeartbeatEntityRepository.findByBox_Id(box.getId()).ifPresentOrElse(
                existing -> {
                    existing.setLastSeenAt(now);
                    existing.setReportedVersion(request.getReportedVersion());
                    existing.setMetadataJson(request.getMetadata());
                    existing.setUpdatedAt(now);
                    boxHeartbeatEntityRepository.save(existing);
                },
                () -> {
                    BoxHeartbeatEntity entity = new BoxHeartbeatEntity();
                    entity.setBox(box);
                    entity.setLastSeenAt(now);
                    entity.setReportedVersion(request.getReportedVersion());
                    entity.setMetadataJson(request.getMetadata());
                    entity.setUpdatedAt(now);
                    boxHeartbeatEntityRepository.save(entity);
                });
    }
}
