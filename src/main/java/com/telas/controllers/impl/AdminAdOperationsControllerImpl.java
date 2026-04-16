package com.telas.controllers.impl;

import com.telas.dtos.request.filters.AdminAdOperationsFilterRequestDto;
import com.telas.dtos.response.AdminAdOperationRowDto;
import com.telas.dtos.response.AdminExpiryNotificationDto;
import com.telas.dtos.response.PaginationResponseDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.AdminAdOperationsService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("admin/ad-operations")
@RequiredArgsConstructor
public class AdminAdOperationsControllerImpl {

    private final AdminAdOperationsService adminAdOperationsService;

    @GetMapping
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<ResponseDto<PaginationResponseDto<List<AdminAdOperationRowDto>>>> list(
            @ModelAttribute AdminAdOperationsFilterRequestDto request) {
        PaginationResponseDto<List<AdminAdOperationRowDto>> data = adminAdOperationsService.findPage(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(data, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @GetMapping("/clients/{advertiserId}/expiry-notifications")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<ResponseDto<List<AdminExpiryNotificationDto>>> expiryNotifications(
            @PathVariable UUID advertiserId) {
        List<AdminExpiryNotificationDto> data = adminAdOperationsService.listExpiryNotifications(advertiserId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(data, HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @GetMapping(value = "/export/subscriptions.csv", produces = "text/csv")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<byte[]> exportSubscriptionsCsv() {
        byte[] bytes = adminAdOperationsService.exportSubscriptionsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=subscriptions-export.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
