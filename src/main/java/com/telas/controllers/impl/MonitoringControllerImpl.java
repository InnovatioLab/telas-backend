package com.telas.controllers.impl;

import com.telas.dtos.request.BoxLogRequestDto;
import com.telas.dtos.request.HeartbeatRequestDto;
import com.telas.dtos.response.IncidentResponseDto;
import com.telas.dtos.response.PaginationResponseDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.ApplicationLogService;
import com.telas.services.BoxHeartbeatService;
import com.telas.services.IncidentQueryService;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("monitoring")
@RequiredArgsConstructor
public class MonitoringControllerImpl {

    private final BoxHeartbeatService boxHeartbeatService;
    private final ApplicationLogService applicationLogService;
    private final IncidentQueryService incidentQueryService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@Valid @RequestBody HeartbeatRequestDto request) {
        boxHeartbeatService.record(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logs")
    public ResponseEntity<Void> boxLogs(@Valid @RequestBody BoxLogRequestDto request) {
        applicationLogService.persistBoxLog(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/incidents")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> incidents(Pageable pageable) {
        authenticatedUserService.validateAdmin();
        Page<IncidentResponseDto> page = incidentQueryService.findAll(pageable);
        PaginationResponseDto<java.util.List<IncidentResponseDto>> body = PaginationResponseDto.fromResult(
                page.getContent(),
                (int) page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber() + 1);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(body, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }
}
