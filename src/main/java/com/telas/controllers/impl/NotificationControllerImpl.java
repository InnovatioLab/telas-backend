package com.telas.controllers.impl;

import com.telas.controllers.NotificationController;
import com.telas.dtos.response.ResponseDto;
import com.telas.entities.Notification;
import com.telas.services.NotificationService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "notifications")
@RequiredArgsConstructor
public class NotificationControllerImpl implements NotificationController {
    private final NotificationService service;

    @Override
    @GetMapping
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> listClientNotifications(
            @RequestParam(name = "ids", required = false) List<UUID> ids,
            Specification<Notification> spec,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(service.listClientNotifications(ids, spec, pageable), HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @Override
    @PatchMapping("/mark-all-read")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> markAllAsRead() {
        service.markAllAsRead();
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> findById(@PathVariable(name = "id") UUID notificationId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(service.findById(notificationId), HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
    }
}
