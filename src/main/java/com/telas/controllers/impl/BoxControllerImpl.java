package com.telas.controllers.impl;

import com.telas.controllers.BoxController;
import com.telas.dtos.request.BoxAddressRequestDto;
import com.telas.dtos.request.BoxRequestDto;
import com.telas.dtos.request.StatusBoxMonitorsRequestDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.BoxAddressService;
import com.telas.services.BoxService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "boxes")
@RequiredArgsConstructor
public class BoxControllerImpl implements BoxController {
    private final BoxService service;
    private final BoxAddressService boxAddressService;


    @Override
    @GetMapping
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> findAll() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(service.findAll(), HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @Override
    @GetMapping("/addresses")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> findAllAvailableAddresses() {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(boxAddressService.findAll(), HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @Override
    @PostMapping
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> save(@Valid @RequestBody BoxRequestDto request) {
        service.save(request, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(null, HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
    }

    @Override
    @PostMapping("/{id}/sync-playlist")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> syncPlaylist(@PathVariable(name = "id") UUID boxId) {
        service.syncPlaylist(boxId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    @PutMapping("/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> update(@Valid @RequestBody BoxRequestDto request, @PathVariable(name = "id") UUID boxId) {
        service.save(request, boxId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @Override
    @GetMapping("/addresses/all")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> findAllAddressesForAdmin() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(boxAddressService.findAllForAdmin(), HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
    }

    @Override
    @PostMapping("/addresses")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> createAddress(@Valid @RequestBody BoxAddressRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(boxAddressService.create(request), HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
    }

    @Override
    @PutMapping("/addresses/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> updateAddress(@Valid @RequestBody BoxAddressRequestDto request, @PathVariable(name = "id") UUID id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(boxAddressService.update(id, request), HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @Override
    @DeleteMapping("/addresses/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> deleteAddress(@PathVariable(name = "id") UUID id) {
        boxAddressService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    @GetMapping("/ads")
    public ResponseEntity<?> getMonitorsAdsByIp(@RequestHeader("X-Box-Address") String address) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(service.getMonitorsAdsByAddress(address), HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
    }

    @Override
    @GetMapping("/player-settings")
    public ResponseEntity<?> getPlayerSettings() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ResponseDto.fromData(
                                service.getPlayerSettings(),
                                HttpStatus.OK,
                                MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
    }

    @Override
    @PostMapping("/health")
    public ResponseEntity<?> updateHealth(@RequestBody StatusBoxMonitorsRequestDto request) {
        service.updateHealth(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ResponseDto.fromData(null, HttpStatus.NO_CONTENT, null));
    }


}
