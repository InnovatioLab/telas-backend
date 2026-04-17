package com.telas.services.impl;

import com.telas.dtos.request.BoxScriptAckRequestDto;
import com.telas.dtos.response.BoxScriptPendingCommandResponseDto;
import com.telas.entities.Box;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.monitoring.entities.BoxScriptUpdateCommandEntity;
import com.telas.monitoring.entities.BoxScriptUpdateCommandStatus;
import com.telas.monitoring.repositories.BoxScriptUpdateCommandEntityRepository;
import com.telas.repositories.BoxRepository;
import com.telas.services.BoxScriptUpdateCommandService;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoxScriptUpdateCommandServiceImpl implements BoxScriptUpdateCommandService {

    private final BoxRepository boxRepository;
    private final BoxScriptUpdateCommandEntityRepository commandRepository;

    @Value("${monitoring.box-script.target-version:}")
    private String configuredTargetVersion;

    @Value("${monitoring.box-script.artifact.url:}")
    private String configuredArtifactUrl;

    @Value("${monitoring.box-script.artifact.sha256:}")
    private String configuredArtifactSha256;

    @Override
    @Transactional
    public void enqueue(UUID boxId) {
        if (!StringUtils.hasText(configuredTargetVersion)) {
            throw new BusinessRuleException(
                    "monitoring.box-script.target-version is not configured");
        }
        if (!StringUtils.hasText(configuredArtifactUrl)
                || !StringUtils.hasText(configuredArtifactSha256)) {
            throw new BusinessRuleException(
                    "monitoring.box-script.artifact.url and monitoring.box-script.artifact.sha256 must be configured");
        }
        Box box =
                boxRepository
                        .findById(boxId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                BoxValidationMessages.BOX_NOT_FOUND));
        if (commandRepository.existsByBox_IdAndStatus(boxId, BoxScriptUpdateCommandStatus.PENDING)) {
            throw new BusinessRuleException(
                    "A box script update is already pending for this box");
        }
        BoxScriptUpdateCommandEntity entity = new BoxScriptUpdateCommandEntity();
        entity.setBox(box);
        entity.setTargetVersion(configuredTargetVersion.trim());
        entity.setArtifactUrl(configuredArtifactUrl.trim());
        entity.setSha256(configuredArtifactSha256.trim());
        entity.setStatus(BoxScriptUpdateCommandStatus.PENDING);
        commandRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BoxScriptPendingCommandResponseDto> pollPending(String boxAddress) {
        if (!StringUtils.hasText(boxAddress)) {
            throw new IllegalArgumentException("boxAddress is required");
        }
        Box box =
                boxRepository
                        .findByAddress(boxAddress.trim())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                BoxValidationMessages.BOX_NOT_FOUND));
        return commandRepository
                .findFirstByBox_IdAndStatusOrderByCreatedAtAsc(
                        box.getId(), BoxScriptUpdateCommandStatus.PENDING)
                .map(
                        c ->
                                BoxScriptPendingCommandResponseDto.builder()
                                        .commandId(c.getId())
                                        .targetVersion(c.getTargetVersion())
                                        .artifactUrl(c.getArtifactUrl())
                                        .sha256(c.getSha256())
                                        .build());
    }

    @Override
    @Transactional
    public void acknowledge(BoxScriptAckRequestDto request) {
        Box box =
                boxRepository
                        .findByAddress(request.getBoxAddress().trim())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                BoxValidationMessages.BOX_NOT_FOUND));
        BoxScriptUpdateCommandEntity cmd =
                commandRepository
                        .findById(request.getCommandId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Box script update command not found"));
        if (!cmd.getBox().getId().equals(box.getId())) {
            throw new BusinessRuleException("Command does not belong to this box");
        }
        if (cmd.getStatus() != BoxScriptUpdateCommandStatus.PENDING) {
            throw new BusinessRuleException("Command is not pending");
        }
        cmd.setCompletedAt(Instant.now());
        if (request.isSuccess()) {
            cmd.setStatus(BoxScriptUpdateCommandStatus.COMPLETED);
        } else {
            cmd.setStatus(BoxScriptUpdateCommandStatus.FAILED);
            cmd.setErrorMessage(
                    StringUtils.hasText(request.getErrorMessage())
                            ? request.getErrorMessage().trim()
                            : "Update failed");
        }
        commandRepository.save(cmd);
    }
}
