package com.telas.controllers.impl;

import com.telas.controllers.AddressController;
import com.telas.dtos.response.AddressFromZipCodeResponseDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "addresses")
@RequiredArgsConstructor
public class AddressControllerImpl implements AddressController {
    private final AddressService addressService;
    private final CacheControl oneDayCacheControl;


    @Override
    @GetMapping("/{zipCode}")
    public ResponseEntity<?> findByZipCode(@PathVariable(name = "zipCode") String zipCode) {
        AddressFromZipCodeResponseDto response = addressService.findByZipCode(zipCode);
        String message = response != null ? "Address found" : "Address not found";
        return ResponseEntity.status(HttpStatus.OK)
                .cacheControl(oneDayCacheControl)
                .body(ResponseDto.fromData(response, HttpStatus.OK, message));
    }
}
