package com.telas.controllers.impl;

import com.telas.dtos.request.SmartPlugIngestRequestDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.SmartPlugIngestService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("monitoring/smart-plugs")
@Tag(name = "Smart plugs ingest", description = "Ingest de leituras do agente/sidecar (API key)")
@RequiredArgsConstructor
public class SmartPlugIngestControllerImpl {

    private final SmartPlugIngestService smartPlugIngestService;

    @PostMapping("/ingest")
    @Operation(summary = "Recebe leitura do agente por box (API key)")
    public ResponseEntity<?> ingest(@Valid @RequestBody SmartPlugIngestRequestDto dto) {
        smartPlugIngestService.ingest(dto);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
    }
}

