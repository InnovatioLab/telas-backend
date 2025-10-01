package com.telas.controllers.impl;

import com.telas.controllers.SubscriptionController;
import com.telas.dtos.request.filters.SubscriptionFilterRequestDto;
import com.telas.dtos.response.PaginationResponseDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.dtos.response.SubscriptionMinResponseDto;
import com.telas.enums.Recurrence;
import com.telas.services.SubscriptionService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "subscriptions")
@RequiredArgsConstructor
public class SubscriptionControllerImpl implements SubscriptionController {
    private final SubscriptionService service;

    @Override
    @PostMapping
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> save() {
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.fromData(service.save(), HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
    }

    @Override
    @GetMapping("/filters")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> findClientSubscriptionsFilters(SubscriptionFilterRequestDto request) {
        PaginationResponseDto<List<SubscriptionMinResponseDto>> response = service.findClientSubscriptionsFilters(request);

        String msg = response.getList().isEmpty() ? MessageCommonsConstants.FIND_FILTER_EMPTY_MESSAGE : MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE;

        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(response, HttpStatus.OK, msg));
    }


    @Override
    @GetMapping("/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> findById(@PathVariable(name = "id") UUID subscriptionId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(service.findById(subscriptionId), HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
    }

    @Override
    @PatchMapping("/renew/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> renewSubscription(@PathVariable(name = "id") UUID subscriptionId) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(service.renewSubscription(subscriptionId), HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @Override
    @PatchMapping("/upgrade/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> upgradeSubscription(
            @PathVariable(name = "id") UUID subscriptionId,
            @RequestParam(name = "recurrence") Recurrence recurrence) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(service.upgradeSubscription(subscriptionId, recurrence), HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @Override
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> cancelSubscription(@PathVariable(name = "id") UUID subscriptionId) {
        service.cancelSubscription(subscriptionId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ResponseDto.fromData(null, HttpStatus.NO_CONTENT, MessageCommonsConstants.SUBSCRIPTION_CANCELLED_SUCCESS_MESSAGE));
    }
}
