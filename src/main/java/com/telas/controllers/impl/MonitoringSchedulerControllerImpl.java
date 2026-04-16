package com.telas.controllers.impl;

import com.telas.dtos.response.ResponseDto;
import com.telas.dtos.response.SchedulerJobStatusResponseDto;
import com.telas.enums.Permission;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.scheduler.SchedulerOverviewService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("monitoring/scheduler")
@Tag(name = "Monitoring scheduler", description = "Scheduled jobs overview")
@RequiredArgsConstructor
public class MonitoringSchedulerControllerImpl {

    private final SchedulerOverviewService schedulerOverviewService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/jobs")
    @Operation(summary = "List scheduled jobs status")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> listJobs() {
        authenticatedUserService.validatePermission(Permission.MONITORING_SCHEDULER_VIEW);
        List<SchedulerJobStatusResponseDto> list = schedulerOverviewService.listJobStatus();
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(list, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }
}
