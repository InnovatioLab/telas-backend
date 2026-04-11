package com.telas.controllers.impl;

import com.telas.dtos.request.BoxLogRequestDto;
import com.telas.dtos.request.HeartbeatRequestDto;
import com.telas.dtos.response.ApplicationLogResponseDto;
import com.telas.dtos.response.IncidentResponseDto;
import com.telas.dtos.response.PaginationResponseDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.ApplicationLogQueryService;
import com.telas.services.ApplicationLogService;
import com.telas.services.BoxHeartbeatService;
import com.telas.services.IncidentQueryService;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("monitoring")
@Tag(name = "Monitoring", description = "Heartbeat, logs de box e incidentes")
@RequiredArgsConstructor
public class MonitoringControllerImpl {

    private final BoxHeartbeatService boxHeartbeatService;
    private final ApplicationLogService applicationLogService;
    private final ApplicationLogQueryService applicationLogQueryService;
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

    @GetMapping("/logs")
    @Operation(summary = "Lista logs de aplicação (admin)")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> applicationLogs(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        authenticatedUserService.validateAdmin();
        Page<ApplicationLogResponseDto> page =
                applicationLogQueryService.findAll(source, level, from, to, q, pageable);
        PaginationResponseDto<java.util.List<ApplicationLogResponseDto>> body =
                PaginationResponseDto.fromResult(
                        page.getContent(),
                        (int) page.getTotalElements(),
                        page.getTotalPages(),
                        page.getNumber() + 1);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(body, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
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
