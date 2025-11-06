package com.telas.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notifications", description = "Endpoints to manage notifications")
public interface NotificationController {
    @Operation(summary = "Endpoint contract to list logged client's notifications", responses = {
            @ApiResponse(responseCode = "200", description = "Notifications founded successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    ResponseEntity<?> listClientNotifications(@RequestParam(name = "ids", required = false) List<UUID> ids);

    @Operation(summary = "Endpoint to fetch notifications by ids", responses = {
            @ApiResponse(responseCode = "200", description = "Notification founded successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden."),
            @ApiResponse(responseCode = "404", description = "Notification(s) not found."),
            @ApiResponse(responseCode = "422", description = "Request with invalid data.")
    })
    ResponseEntity<?> findById(UUID notificationId);
}
